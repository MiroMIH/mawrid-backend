package com.mawrid.category;

import com.mawrid.category.dto.*;
import com.mawrid.common.enums.AttributeType;
import com.mawrid.common.enums.NodeType;
import com.mawrid.common.exception.BusinessException;
import com.mawrid.common.exception.DuplicateResourceException;
import com.mawrid.demande.Demande;
import com.mawrid.demande.DemandeAttribute;
import com.mawrid.demande.DemandeAttributeRepository;
import com.mawrid.demande.DemandeRepository;
import com.mawrid.demande.DemandeStatus;
import com.mawrid.user.Role;
import com.mawrid.user.User;
import com.mawrid.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for CategoryService.
 * Requires a local PostgreSQL instance with database 'mawrid_test_tc':
 *   CREATE DATABASE mawrid_test_tc;
 * Uses the same credentials as the dev database (mawrid / mawrid_dev_pass_123).
 * All test data is rolled back via @Transactional.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/mawrid_test_tc",
        "spring.datasource.username=mawrid",
        "spring.datasource.password=mawrid_dev_pass_123",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.cache.type=simple",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.data.redis.port=6399",
        "spring.jpa.properties.hibernate.format_sql=false",
        "spring.jpa.show-sql=false",
})
class CategoryIntegrationTest {

    @MockBean
    StringRedisTemplate stringRedisTemplate;

    @Autowired CategoryService categoryService;
    @Autowired CategoryRepository categoryRepository;
    @Autowired CategoryAttributeRepository attributeRepository;
    @Autowired UserRepository userRepository;
    @Autowired DemandeRepository demandeRepository;
    @Autowired DemandeAttributeRepository demandeAttributeRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired CacheManager cacheManager;

    @BeforeEach
    void setUpMocks() {
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);
        // Clear category cache so each test sees fresh data
        cacheManager.getCacheNames().forEach(name ->
                cacheManager.getCache(name).clear());
    }

    // ── Helper factories ──────────────────────────────────────────

    private Category createRoot(String name) {
        CategoryCreateRequest req = new CategoryCreateRequest();
        req.setName(name);
        Long id = categoryService.create(req).getId();
        return categoryRepository.findById(id).orElseThrow();
    }

    private Category createChild(String name, Long parentId) {
        CategoryCreateRequest req = new CategoryCreateRequest();
        req.setName(name);
        req.setParentId(parentId);
        Long id = categoryService.create(req).getId();
        return categoryRepository.findById(id).orElseThrow();
    }

    private User createSupplier() {
        return userRepository.save(User.builder()
                .email(UUID.randomUUID() + "@test.dz")
                .password(passwordEncoder.encode("Password1!"))
                .firstName("Supplier")
                .lastName("Test")
                .role(Role.SUPPLIER)
                .enabled(true)
                .build());
    }

    private Demande createDemande(User buyer, Category category) {
        return demandeRepository.save(Demande.builder()
                .title("Test Demande")
                .description("desc")
                .quantity(10)
                .unit("pcs")
                .deadline(LocalDate.now().plusDays(30))
                .status(DemandeStatus.OPEN)
                .category(category)
                .buyer(buyer)
                .wilaya("16")
                .qualityScore(50)
                .build());
    }

    // ── Test 1: createCategory_success ───────────────────────────

    @Test
    void createCategory_success_computesPathCorrectly() {
        Category root = createRoot("Secteur Test");
        assertThat(root.getPath()).isEqualTo(String.valueOf(root.getId()));
        assertThat(root.getDepth()).isEqualTo(0);
        assertThat(root.getNodeType()).isEqualTo(NodeType.ADMIN_CREATED);

        Category child = createChild("Sous-secteur A", root.getId());
        assertThat(child.getPath()).isEqualTo(root.getId() + "." + child.getId());
        assertThat(child.getDepth()).isEqualTo(1);

        Category grandchild = createChild("Sous-sous-secteur B", child.getId());
        assertThat(grandchild.getPath()).isEqualTo(root.getId() + "." + child.getId() + "." + grandchild.getId());
        assertThat(grandchild.getDepth()).isEqualTo(2);
    }

    // ── Test 2: createCategory_duplicateSiblingName_throwsConflict

    @Test
    void createCategory_duplicateSiblingName_throwsConflict() {
        Category root = createRoot("Équipements Divers");
        createChild("Ventilateurs", root.getId());

        CategoryCreateRequest dup = new CategoryCreateRequest();
        dup.setName("ventilateurs"); // same name, different case
        dup.setParentId(root.getId());

        assertThatThrownBy(() -> categoryService.create(dup))
                .isInstanceOf(DuplicateResourceException.class);
    }

    // ── Test 3: createCategory_underLeafNode_throwsBusinessException

    @Test
    void createCategory_underLeafNode_throwsBusinessException() {
        Category root = createRoot("Matières Premières");
        Category leaf = createChild("Aluminium brut", root.getId());
        categoryService.markAsLeaf(leaf.getId());

        CategoryCreateRequest req = new CategoryCreateRequest();
        req.setName("Aluminium 6061");
        req.setParentId(leaf.getId());

        assertThatThrownBy(() -> categoryService.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("LEAF");
    }

    // ── Test 4: renameCategory_seededNode_withoutFlag_throwsBusinessException

    @Test
    void renameCategory_seededNode_withoutFlag_throwsBusinessException() {
        // Seed a SEEDED node directly via repository
        Category seeded = categoryRepository.save(Category.builder()
                .name("Secteur Seeded")
                .slug("secteur-seeded-test")
                .path("placeholder")
                .depth(0)
                .nodeType(NodeType.SEEDED)
                .build());
        seeded.setPath(String.valueOf(seeded.getId()));
        categoryRepository.save(seeded);

        CategoryRenameRequest req = new CategoryRenameRequest();
        req.setName("Secteur Renommé");
        req.setForceRename(false);

        assertThatThrownBy(() -> categoryService.rename(seeded.getId(), req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SEEDED");
    }

    // ── Test 5: moveCategory_createsCorrectPathsForAllDescendants ─

    @Test
    void moveCategory_createsCorrectPathsForAllDescendants() {
        Category rootA = createRoot("Secteur A");
        Category rootB = createRoot("Secteur B");
        Category child = createChild("Enfant 1", rootA.getId());
        Category grandchild = createChild("Petit-enfant 1", child.getId());

        // Move child (and its descendant) from rootA to rootB
        categoryService.move(child.getId(), rootB.getId());

        Category movedChild = categoryRepository.findById(child.getId()).orElseThrow();
        Category movedGrandchild = categoryRepository.findById(grandchild.getId()).orElseThrow();

        String expectedChildPath = rootB.getId() + "." + child.getId();
        String expectedGrandchildPath = rootB.getId() + "." + child.getId() + "." + grandchild.getId();

        assertThat(movedChild.getPath()).isEqualTo(expectedChildPath);
        assertThat(movedChild.getDepth()).isEqualTo(1);
        assertThat(movedGrandchild.getPath()).isEqualTo(expectedGrandchildPath);
        assertThat(movedGrandchild.getDepth()).isEqualTo(2);
    }

    // ── Test 6: moveCategory_toDescendant_throwsBusinessException ─

    @Test
    void moveCategory_toDescendant_throwsBusinessException() {
        Category root = createRoot("Secteur Hiérarchique");
        Category child = createChild("Enfant", root.getId());
        Category grandchild = createChild("Petit-Enfant", child.getId());

        // Attempt to move 'root' into its own grandchild — cycle
        assertThatThrownBy(() -> categoryService.move(root.getId(), grandchild.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("subtree");
    }

    // ── Test 7: toggleActive_nodeWithActiveDemandes_throwsBusinessException

    @Test
    void toggleActive_nodeWithActiveDemandes_throwsBusinessException() {
        Category root = createRoot("Secteur Actif");
        Category cat = createChild("Catégorie Active", root.getId());

        // Manually set demandeCount > 0 to simulate active demandes
        cat.setDemandeCount(3);
        categoryRepository.save(cat);

        assertThatThrownBy(() -> categoryService.toggleActive(cat.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("3");
    }

    // ── Test 8: toggleActive_deactivatesAllDescendants ────────────

    @Test
    void toggleActive_deactivatesAllDescendants() {
        Category root = createRoot("Secteur Parent");
        Category child = createChild("Enfant Actif", root.getId());
        Category grandchild = createChild("Petit-Enfant Actif", child.getId());

        // Deactivate root → should cascade to child and grandchild
        categoryService.toggleActive(root.getId());

        Category freshRoot = categoryRepository.findById(root.getId()).orElseThrow();
        Category freshChild = categoryRepository.findById(child.getId()).orElseThrow();
        Category freshGrandchild = categoryRepository.findById(grandchild.getId()).orElseThrow();

        assertThat(freshRoot.isActive()).isFalse();
        assertThat(freshChild.isActive()).isFalse();
        assertThat(freshGrandchild.isActive()).isFalse();
    }

    // ── Test 9: getAttributes_returnsInheritedAndOwnMerged ────────

    @Test
    void getAttributes_returnsInheritedAndOwnMerged() {
        Category root = createRoot("Pièces Mécaniques Test");
        Category child = createChild("Roulements Test", root.getId());

        // Add attribute on root (will be inherited by child)
        CategoryAttributeRequest parentAttr = new CategoryAttributeRequest();
        parentAttr.setKey("marque");
        parentAttr.setLabel("Marque");
        parentAttr.setType(AttributeType.TEXT);
        categoryService.addAttribute(root.getId(), parentAttr);

        // Add attribute on child (own)
        CategoryAttributeRequest childAttr = new CategoryAttributeRequest();
        childAttr.setKey("reference");
        childAttr.setLabel("Référence");
        childAttr.setType(AttributeType.TEXT);
        categoryService.addAttribute(child.getId(), childAttr);

        List<CategoryAttributeResponse> attrs = categoryService.getAttributes(child.getId());

        assertThat(attrs).hasSize(2);
        CategoryAttributeResponse marque = attrs.stream()
                .filter(a -> a.getKey().equals("marque")).findFirst().orElseThrow();
        CategoryAttributeResponse reference = attrs.stream()
                .filter(a -> a.getKey().equals("reference")).findFirst().orElseThrow();

        assertThat(marque.isInherited()).isTrue();
        assertThat(reference.isInherited()).isFalse();
    }

    // ── Test 10: deleteAttribute_usedInDemandes_throwsBusinessException

    @Test
    void deleteAttribute_usedInDemandes_throwsBusinessException() {
        Category root = createRoot("Secteur Attributs");

        CategoryAttributeRequest attrReq = new CategoryAttributeRequest();
        attrReq.setKey("alesage");
        attrReq.setLabel("Alésage (mm)");
        attrReq.setType(AttributeType.NUMBER);
        CategoryAttributeResponse attrResp = categoryService.addAttribute(root.getId(), attrReq);

        // Create a user and demande that references this attribute key
        User buyer = userRepository.save(User.builder()
                .email(UUID.randomUUID() + "@buyer.dz")
                .password(passwordEncoder.encode("Pass1!"))
                .firstName("Acheteur")
                .lastName("Test")
                .role(Role.BUYER)
                .enabled(true)
                .build());

        Demande demande = createDemande(buyer, root);

        demandeAttributeRepository.save(DemandeAttribute.builder()
                .demande(demande)
                .key("alesage")
                .value("45")
                .custom(false)
                .build());

        assertThatThrownBy(() -> categoryService.deleteAttribute(root.getId(), attrResp.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("alesage");
    }

    // ── Test 11: getTree_returnsOnlyActiveNodes ───────────────────

    @Test
    void getTree_returnsOnlyActiveNodes() {
        Category visible = createRoot("Secteur Visible");
        Category hidden = createRoot("Secteur Caché");

        // Deactivate hidden sector (it has no demandes, not SEEDED root, not depth=0 restriction
        // but we created it as ADMIN_CREATED so toggle is allowed)
        categoryService.toggleActive(hidden.getId());

        List<CategoryTreeResponse> tree = categoryService.getTree();

        List<Long> ids = tree.stream().map(CategoryTreeResponse::getId).toList();
        assertThat(ids).contains(visible.getId());
        assertThat(ids).doesNotContain(hidden.getId());
    }

    // ── Test 12: moveCategory_supplierSubscriptionsIntact ─────────

    @Test
    void moveCategory_supplierSubscriptionsIntact() {
        Category rootA = createRoot("Secteur Source");
        Category rootB = createRoot("Secteur Destination");
        Category child = createChild("Produits Déplacés", rootA.getId());

        // Supplier subscribes to 'child' category
        User supplier = createSupplier();
        Category managedChild = categoryRepository.findById(child.getId()).orElseThrow();
        supplier.getCategories().add(managedChild);
        userRepository.save(supplier);

        // Move child to rootB
        categoryService.move(child.getId(), rootB.getId());

        // Verify: supplier still has the subscription to 'child' (ID unchanged)
        User reloaded = userRepository.findById(supplier.getId()).orElseThrow();
        assertThat(reloaded.getCategories())
                .extracting(Category::getId)
                .contains(child.getId());

        // And the child's path has been updated
        Category movedChild = categoryRepository.findById(child.getId()).orElseThrow();
        assertThat(movedChild.getPath()).startsWith(rootB.getId() + ".");
    }
}
