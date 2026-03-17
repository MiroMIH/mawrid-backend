package com.mawrid.category;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mawrid.category.dto.*;
import com.mawrid.common.enums.AttributeType;
import com.mawrid.common.enums.NodeType;
import com.mawrid.common.exception.BusinessException;
import com.mawrid.common.exception.DuplicateResourceException;
import com.mawrid.common.exception.ResourceNotFoundException;
import com.mawrid.common.util.SlugUtils;
import com.mawrid.demande.DemandeAttributeRepository;
import com.mawrid.user.User;
import com.mawrid.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryAttributeRepository attributeRepository;
    private final DemandeAttributeRepository demandeAttributeRepository;
    private final UserRepository userRepository;
    private final CategoryMapper categoryMapper;
    private final ObjectMapper objectMapper;

    // ── Tree ─────────────────────────────────────────────────────

    /**
     * Returns the full active tree as a nested structure.
     * Loads all active categories in ONE query, then builds the tree in Java memory.
     */
    @Cacheable("categories")
    @Transactional(readOnly = true)
    public List<CategoryTreeResponse> getTree() {
        List<Category> all = categoryRepository.findAllActive();
        Map<Long, List<Category>> childrenByParentId = all.stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));
        return all.stream()
                .filter(c -> c.getParent() == null)
                .map(root -> buildTreeNode(root, childrenByParentId))
                .toList();
    }

    private CategoryTreeResponse buildTreeNode(Category node, Map<Long, List<Category>> childrenMap) {
        List<CategoryTreeResponse> children = childrenMap.getOrDefault(node.getId(), List.of())
                .stream()
                .map(child -> buildTreeNode(child, childrenMap))
                .sorted(Comparator.comparing(CategoryTreeResponse::getName))
                .toList();
        return CategoryTreeResponse.builder()
                .id(node.getId())
                .name(node.getName())
                .slug(node.getSlug())
                .depth(node.getDepth())
                .nodeType(node.getNodeType())
                .active(node.isActive())
                .demandeCount(node.getDemandeCount())
                .children(children)
                .build();
    }

    // ── Single node ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        Category c = findOrThrow(id);
        if (!c.isActive()) throw new ResourceNotFoundException("Category", id);
        return categoryMapper.toResponse(c);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getBySlug(String slug) {
        Category c = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));
        if (!c.isActive()) throw new ResourceNotFoundException("Category not found with slug: " + slug);
        return categoryMapper.toResponse(c);
    }

    // ── Subscribed categories (SUPPLIER) ─────────────────────────

    @Transactional(readOnly = true)
    public List<CategoryResponse> getSubscribed(User user) {
        User managed = userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", user.getId()));
        return managed.getCategories().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    // ── Attributes ───────────────────────────────────────────────

    /**
     * Returns ALL effective attributes for a node: inherited from ancestors (marked inherited=true)
     * plus own attributes (inherited=false). Deduplicated by key — own overrides ancestor.
     */
    @Transactional(readOnly = true)
    public List<CategoryAttributeResponse> getAttributes(Long categoryId) {
        Category category = findOrThrow(categoryId);
        List<CategoryAttribute> raw = attributeRepository.findAllEffectiveForCategory(category.getPath());

        // Merge: iterate depth ASC (ancestors first), build map keyed by attribute key.
        // Later entries (deeper = own) override earlier (shallower = ancestor).
        LinkedHashMap<String, CategoryAttribute> byKey = new LinkedHashMap<>();
        for (CategoryAttribute a : raw) {
            byKey.put(a.getKey(), a);
        }

        return byKey.values().stream()
                .sorted(Comparator.comparingInt(CategoryAttribute::getDisplayOrder))
                .map(a -> {
                    boolean inherited = !a.getCategory().getId().equals(categoryId);
                    return buildAttributeResponse(a, inherited);
                })
                .toList();
    }

    // ── Create ────────────────────────────────────────────────────

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public CategoryResponse create(CategoryCreateRequest request) {
        // Validate name
        if (request.getName().contains(".")) {
            throw new BusinessException("Category name cannot contain dots");
        }

        Category parent = null;
        int depth = 0;

        if (request.getParentId() != null) {
            parent = findOrThrow(request.getParentId());

            if (!parent.isActive()) {
                throw new BusinessException("Cannot create a child under an inactive parent");
            }
            if (parent.getNodeType() == NodeType.LEAF) {
                throw new BusinessException("Cannot create a child under a LEAF node — leaf nodes are terminal");
            }
            if (existsSiblingWithName(parent, request.getName())) {
                throw new DuplicateResourceException("Category", "name", request.getName());
            }

            depth = parent.getDepth() + 1;
            if (depth > 10) {
                log.warn("Creating category at depth {} — this exceeds the soft limit of 10", depth);
            }
        } else {
            if (categoryRepository.existsByParentNullAndNameIgnoreCase(request.getName())) {
                throw new DuplicateResourceException("Category", "name", request.getName());
            }
        }

        // Auto-generate unique slug
        String slug = SlugUtils.toUniqueSlug(request.getName(), categoryRepository::existsBySlug);
        NodeType nodeType = request.getNodeType() != null ? request.getNodeType() : NodeType.ADMIN_CREATED;

        // Save once to get the auto-generated ID
        Category saved = categoryRepository.save(
                Category.builder()
                        .name(request.getName())
                        .slug(slug)
                        .parent(parent)
                        .path("placeholder")
                        .depth(depth)
                        .nodeType(nodeType)
                        .build()
        );

        // Now compute the materialized path using the real ID
        String path = (parent == null)
                ? String.valueOf(saved.getId())
                : parent.getPath() + "." + saved.getId();
        saved.setPath(path);

        return categoryMapper.toResponse(categoryRepository.save(saved));
    }

    // ── Rename ────────────────────────────────────────────────────

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public CategoryResponse rename(Long id, CategoryRenameRequest request) {
        Category category = findOrThrow(id);

        if (category.getNodeType() == NodeType.SEEDED && !request.isForceRename()) {
            throw new BusinessException(
                    "SEEDED categories cannot be renamed without the forceRename flag (superadmin only)");
        }
        if (request.getName().contains(".")) {
            throw new BusinessException("Category name cannot contain dots");
        }
        // Check sibling name uniqueness
        if (category.getParent() != null) {
            if (existsSiblingWithName(category.getParent(), request.getName()) &&
                    !category.getName().equalsIgnoreCase(request.getName())) {
                throw new DuplicateResourceException("Category", "name", request.getName());
            }
        } else {
            if (categoryRepository.existsByParentNullAndNameIgnoreCase(request.getName()) &&
                    !category.getName().equalsIgnoreCase(request.getName())) {
                throw new DuplicateResourceException("Category", "name", request.getName());
            }
        }

        category.setName(request.getName());
        // Slug is immutable — do NOT update it on rename
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    // ── Toggle active ─────────────────────────────────────────────

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public CategoryResponse toggleActive(Long id) {
        Category category = findOrThrow(id);

        if (category.isActive()) {
            // Deactivating
            if (category.getNodeType() == NodeType.SEEDED && category.getDepth() == 0) {
                throw new BusinessException("Root SEEDED sectors cannot be deactivated");
            }
            if (category.getDemandeCount() > 0) {
                throw new BusinessException(
                        "Cannot deactivate category '" + category.getName() +
                        "' — it has " + category.getDemandeCount() + " active or historical demande(s). " +
                        "Close or cancel those demandes first.");
            }
            // Cascade: deactivate all descendants too
            List<Category> descendants = categoryRepository.findAllByPathStartingWith(category.getPath());
            descendants.forEach(d -> d.setActive(false));
            categoryRepository.saveAll(descendants);
        } else {
            // Activating
            if (category.getParent() != null && !category.getParent().isActive()) {
                throw new BusinessException(
                        "Cannot activate a node whose parent is inactive. Activate the parent first.");
            }
            category.setActive(true);
            categoryRepository.save(category);
        }

        return categoryMapper.toResponse(categoryRepository.findById(id).orElseThrow());
    }

    // ── Move ─────────────────────────────────────────────────────

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public CategoryResponse move(Long nodeId, Long newParentId) {
        Category node = findOrThrow(nodeId);
        Category newParent = findOrThrow(newParentId);

        if (!newParent.isActive()) {
            throw new BusinessException("Target parent is inactive");
        }
        if (nodeId.equals(newParentId)) {
            throw new BusinessException("Cannot move a category into itself");
        }
        // Cycle detection: newParent must not be a descendant of node
        if (newParent.getPath().equals(node.getPath()) ||
                newParent.getPath().startsWith(node.getPath() + ".")) {
            throw new BusinessException("Cannot move a category into its own subtree");
        }
        if (newParent.getNodeType() == NodeType.LEAF) {
            throw new BusinessException("Cannot move a category into a LEAF node");
        }
        if (node.getNodeType() == NodeType.SEEDED && node.getDepth() == 0) {
            throw new BusinessException("Root SEEDED sectors cannot be moved");
        }
        // Sibling name conflict at new location
        if (existsSiblingWithName(newParent, node.getName())) {
            throw new DuplicateResourceException("Category", "name", node.getName());
        }

        String oldPathPrefix = node.getPath();
        String newNodePath = newParent.getPath() + "." + node.getId();
        int newDepth = newParent.getDepth() + 1;

        // Load all descendants
        List<Category> descendants = categoryRepository.findAllByPathStartingWith(oldPathPrefix);

        // Rewrite paths for all nodes in the subtree (including the node itself)
        for (Category c : descendants) {
            String updatedPath = newNodePath + c.getPath().substring(oldPathPrefix.length());
            int depthDelta = newDepth - node.getDepth();
            c.setPath(updatedPath);
            c.setDepth(c.getDepth() + depthDelta);
        }
        categoryRepository.saveAll(descendants);

        // Update parent reference on the moved node
        node.setParent(newParent);
        Category saved = categoryRepository.save(node);

        log.info("Admin moved category id={} '{}' from path='{}' to path='{}'",
                node.getId(), node.getName(), oldPathPrefix, newNodePath);

        return categoryMapper.toResponse(saved);
    }

    // ── Delete ────────────────────────────────────────────────────

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public void delete(Long id) {
        Category category = findOrThrow(id);

        if (category.getNodeType() == NodeType.SEEDED) {
            throw new BusinessException("SEEDED categories cannot be hard-deleted. Use toggle-active to deactivate them.");
        }
        if (!category.getChildren().isEmpty()) {
            throw new BusinessException(
                    "Cannot delete category '" + category.getName() +
                    "' — it has " + category.getChildren().size() + " child node(s). Delete or move them first.");
        }
        if (category.getDemandeCount() > 0) {
            throw new BusinessException(
                    "Cannot delete category '" + category.getName() +
                    "' — it has " + category.getDemandeCount() + " associated demande(s).");
        }

        // Remove own attributes first
        List<CategoryAttribute> ownAttrs = attributeRepository.findByCategoryOrderByDisplayOrderAsc(category);
        attributeRepository.deleteAll(ownAttrs);

        categoryRepository.delete(category);
    }

    // ── Leaf management ───────────────────────────────────────────

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public CategoryResponse markAsLeaf(Long id) {
        Category category = findOrThrow(id);
        if (!category.getChildren().isEmpty()) {
            throw new BusinessException(
                    "Cannot mark as LEAF — category has " + category.getChildren().size() + " children");
        }
        category.setNodeType(NodeType.LEAF);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public CategoryResponse unmarkAsLeaf(Long id) {
        Category category = findOrThrow(id);
        if (category.getNodeType() != NodeType.LEAF) {
            throw new BusinessException("Category is not currently a LEAF");
        }
        category.setNodeType(NodeType.ADMIN_CREATED);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    // ── Attributes: add ──────────────────────────────────────────

    @Transactional
    public CategoryAttributeResponse addAttribute(Long categoryId, CategoryAttributeRequest request) {
        Category category = findOrThrow(categoryId);

        // Validate SELECT type has options
        if (request.getType() == AttributeType.SELECT) {
            if (request.getOptions() == null || request.getOptions().size() < 2) {
                throw new BusinessException("SELECT attribute must have at least 2 options");
            }
        }

        // Key must be unique within the effective attribute set
        List<CategoryAttribute> effective = attributeRepository.findAllEffectiveForCategory(category.getPath());
        boolean keyConflict = effective.stream()
                .anyMatch(a -> a.getKey().equals(request.getKey()));
        if (keyConflict) {
            throw new DuplicateResourceException("CategoryAttribute", "key", request.getKey());
        }

        // Auto-assign displayOrder if not provided (0 means use next available)
        int displayOrder = request.getDisplayOrder();
        if (displayOrder == 0) {
            displayOrder = attributeRepository.findMaxDisplayOrderByCategory(category) + 1;
        }

        String optionsJson = serializeOptions(request);

        CategoryAttribute attr = CategoryAttribute.builder()
                .category(category)
                .key(request.getKey())
                .label(request.getLabel())
                .type(request.getType())
                .required(request.isRequired())
                .displayOrder(displayOrder)
                .options(optionsJson)
                .inherited(false)
                .build();

        return buildAttributeResponse(attributeRepository.save(attr), false);
    }

    // ── Attributes: update ────────────────────────────────────────

    @Transactional
    public CategoryAttributeResponse updateAttribute(Long categoryId, Long attributeId,
                                                     CategoryAttributeRequest request) {
        Category category = findOrThrow(categoryId);
        CategoryAttribute attr = attributeRepository.findById(attributeId)
                .orElseThrow(() -> new ResourceNotFoundException("CategoryAttribute", attributeId));

        // Must belong directly to this category (not inherited)
        if (!attr.getCategory().getId().equals(categoryId)) {
            throw new BusinessException("Cannot update an inherited attribute. It belongs to category id=" +
                    attr.getCategory().getId());
        }

        // Key is immutable
        if (!attr.getKey().equals(request.getKey()) && request.getKey() != null) {
            throw new BusinessException("Attribute key is immutable and cannot be changed");
        }

        if (request.getType() == AttributeType.SELECT) {
            if (request.getOptions() == null || request.getOptions().size() < 2) {
                throw new BusinessException("SELECT attribute must have at least 2 options");
            }
        }

        // If type changes away from SELECT, clear options
        if (request.getType() != AttributeType.SELECT) {
            attr.setOptions(null);
        } else {
            attr.setOptions(serializeOptions(request));
        }

        attr.setLabel(request.getLabel());
        attr.setType(request.getType());
        attr.setRequired(request.isRequired());
        attr.setDisplayOrder(request.getDisplayOrder());

        return buildAttributeResponse(attributeRepository.save(attr), false);
    }

    // ── Attributes: delete ────────────────────────────────────────

    @Transactional
    public void deleteAttribute(Long categoryId, Long attributeId) {
        CategoryAttribute attr = attributeRepository.findById(attributeId)
                .orElseThrow(() -> new ResourceNotFoundException("CategoryAttribute", attributeId));

        if (!attr.getCategory().getId().equals(categoryId)) {
            throw new BusinessException("Cannot delete an inherited attribute. It belongs to category id=" +
                    attr.getCategory().getId());
        }

        long usageCount = demandeAttributeRepository.countByKey(attr.getKey());
        if (usageCount > 0) {
            throw new BusinessException(
                    "Cannot delete attribute '" + attr.getKey() + "' — it has been used in " +
                    usageCount + " demande attribute record(s). Remove those records first.");
        }

        if (attr.isRequired()) {
            log.warn("Deleting required attribute '{}' from category id={} — existing demandes may be missing this field",
                    attr.getKey(), categoryId);
        }

        attributeRepository.delete(attr);
    }

    // ── Stats ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CategoryStatsResponse getStats(Long id) {
        Category category = findOrThrow(id);

        List<Category> subtree = categoryRepository.findAllByPathStartingWith(category.getPath());
        List<Long> subtreeIds = subtree.stream().map(Category::getId).toList();

        long totalDemandesInSubtree = subtree.stream().mapToLong(Category::getDemandeCount).sum();
        long activeSuppliersInSubtree = userRepository.countActiveSuppliersInCategories(subtreeIds);
        long activeSuppliers = userRepository.countActiveSuppliersForCategory(category.getId());

        boolean hasActiveChildren = category.getChildren().stream().anyMatch(Category::isActive);

        return CategoryStatsResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .totalDemandes(category.getDemandeCount())
                .totalDemandesInSubtree(totalDemandesInSubtree)
                .activeSuppliers(activeSuppliers)
                .totalSuppliersInSubtree(activeSuppliersInSubtree)
                .childrenCount(category.getChildren().size())
                .depth(category.getDepth())
                .hasActiveChildren(hasActiveChildren)
                .build();
    }

    // ── Search ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<CategoryResponse> search(String q, Integer depth, NodeType nodeType,
                                         Boolean active, Pageable pageable) {
        String qParam = (q != null && !q.isBlank()) ? q : null;
        return categoryRepository.search(qParam, depth, nodeType, active, pageable)
                .map(categoryMapper::toResponse);
    }

    // ── Helpers ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Category findOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    private boolean existsSiblingWithName(Category parent, String name) {
        return categoryRepository.existsByParentAndNameIgnoreCase(parent, name);
    }

    private CategoryAttributeResponse buildAttributeResponse(CategoryAttribute attr, boolean inherited) {
        List<String> parsedOptions = null;
        if (attr.getOptions() != null) {
            try {
                parsedOptions = objectMapper.readValue(attr.getOptions(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            } catch (Exception e) {
                parsedOptions = null;
            }
        }
        return CategoryAttributeResponse.builder()
                .id(attr.getId())
                .categoryId(attr.getCategory().getId())
                .key(attr.getKey())
                .label(attr.getLabel())
                .type(attr.getType())
                .required(attr.isRequired())
                .inherited(inherited)
                .displayOrder(attr.getDisplayOrder())
                .options(parsedOptions)
                .build();
    }

    private String serializeOptions(CategoryAttributeRequest request) {
        if (request.getType() != AttributeType.SELECT || request.getOptions() == null) return null;
        try {
            return objectMapper.writeValueAsString(request.getOptions());
        } catch (JsonProcessingException e) {
            throw new BusinessException("Invalid options format");
        }
    }
}
