package com.mawrid.category;

import com.mawrid.category.dto.CategoryAttributeRequest;
import com.mawrid.category.dto.CategoryAttributeResponse;
import com.mawrid.category.dto.CategoryRequest;
import com.mawrid.category.dto.CategoryResponse;
import com.mawrid.common.enums.NodeType;
import com.mawrid.common.exception.BusinessException;
import com.mawrid.common.exception.DuplicateResourceException;
import com.mawrid.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryAttributeRepository attributeRepository;
    private final CategoryMapper categoryMapper;

    // ── Tree ─────────────────────────────────────────────────────

    @Cacheable("categories")
    @Transactional(readOnly = true)
    public List<CategoryResponse> getTree() {
        return categoryRepository.findByParentIsNull().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    // ── CRUD ─────────────────────────────────────────────────────

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsBySlug(request.getSlug())) {
            throw new DuplicateResourceException("Category", "slug", request.getSlug());
        }

        Category parent = null;
        String path;
        int depth;

        if (request.getParentId() != null) {
            parent = findOrThrow(request.getParentId());
            Category saved = categoryRepository.save(
                    Category.builder()
                            .name(request.getName())
                            .slug(request.getSlug())
                            .parent(parent)
                            .path("placeholder") // temp — need ID first
                            .depth(parent.getDepth() + 1)
                            .nodeType(NodeType.ADMIN_CREATED)
                            .build()
            );
            // Now we have the ID, compute real path
            saved.setPath(parent.getPath() + "." + saved.getId());
            return categoryMapper.toResponse(categoryRepository.save(saved));
        } else {
            Category saved = categoryRepository.save(
                    Category.builder()
                            .name(request.getName())
                            .slug(request.getSlug())
                            .parent(null)
                            .path("placeholder")
                            .depth(0)
                            .nodeType(NodeType.ADMIN_CREATED)
                            .build()
            );
            saved.setPath(String.valueOf(saved.getId()));
            return categoryMapper.toResponse(categoryRepository.save(saved));
        }
    }

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findOrThrow(id);

        if (category.getNodeType() == NodeType.SEEDED) {
            throw new BusinessException("Cannot rename SEEDED categories");
        }

        if (!category.getSlug().equals(request.getSlug()) &&
                categoryRepository.existsBySlug(request.getSlug())) {
            throw new DuplicateResourceException("Category", "slug", request.getSlug());
        }

        category.setName(request.getName());
        category.setSlug(request.getSlug());
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    // ── Attributes ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CategoryAttributeResponse> getAttributes(Long categoryId) {
        Category category = findOrThrow(categoryId);
        // Returns both own attributes and inherited attributes from ancestors
        return attributeRepository.findAllEffectiveForCategory(category.getPath())
                .stream()
                .map(a -> {
                    CategoryAttributeResponse resp = categoryMapper.toAttributeResponse(a);
                    return resp;
                })
                .toList();
    }

    @Transactional
    public CategoryAttributeResponse addAttribute(Long categoryId, CategoryAttributeRequest request) {
        Category category = findOrThrow(categoryId);

        CategoryAttribute attr = CategoryAttribute.builder()
                .category(category)
                .key(request.getKey())
                .label(request.getLabel())
                .type(request.getType())
                .required(request.isRequired())
                .displayOrder(request.getDisplayOrder())
                .inherited(false)
                .build();

        return categoryMapper.toAttributeResponse(attributeRepository.save(attr));
    }

    // ── Move ─────────────────────────────────────────────────────

    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public CategoryResponse move(Long id, Long newParentId) {
        Category category = findOrThrow(id);
        Category newParent = findOrThrow(newParentId);

        if (newParent.getPath().startsWith(category.getPath())) {
            throw new BusinessException("Cannot move a category into its own subtree");
        }

        String oldPathPrefix = category.getPath();
        String newPath = newParent.getPath() + "." + category.getId();
        int newDepth = newParent.getDepth() + 1;

        // Update this category
        category.setParent(newParent);
        category.setPath(newPath);
        category.setDepth(newDepth);
        categoryRepository.save(category);

        // Rewrite all descendant paths
        rewriteDescendantPaths(category, oldPathPrefix, newPath, newDepth);

        return categoryMapper.toResponse(category);
    }

    private void rewriteDescendantPaths(Category parent, String oldPrefix, String newPrefix, int parentDepth) {
        for (Category child : parent.getChildren()) {
            String childNewPath = newPrefix + "." + child.getId();
            child.setPath(childNewPath);
            child.setDepth(parentDepth + 1);
            categoryRepository.save(child);
            rewriteDescendantPaths(child, oldPrefix, childNewPath, parentDepth + 1);
        }
    }

    // ── Helpers ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Category findOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }
}
