package com.mawrid.category;

import com.mawrid.common.enums.NodeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // ── Legacy / backward-compat ─────────────────────────────────
    List<Category> findByParentIsNull();
    boolean existsBySlug(String slug);
    Optional<Category> findBySlug(String slug);

    // ── Tree queries ─────────────────────────────────────────────

    /** All active categories ordered by path (for single-query tree build) */
    @Query("SELECT c FROM Category c WHERE c.active = true ORDER BY c.path")
    List<Category> findAllActive();

    /** All categories (incl. inactive) ordered by path */
    @Query("SELECT c FROM Category c ORDER BY c.path")
    List<Category> findAllOrderedByPath();

    // ── Path-based queries ────────────────────────────────────────

    Optional<Category> findByPath(String path);

    /**
     * All descendants of a node: exact match OR children at any depth.
     * SQL equivalent: WHERE path = :prefix OR path LIKE ':prefix.%'
     */
    @Query("SELECT c FROM Category c WHERE c.path = :prefix OR c.path LIKE CONCAT(:prefix, '.%') ORDER BY c.path")
    List<Category> findAllByPathStartingWith(@Param("prefix") String prefix);

    /**
     * All ancestors of a node, identified by the IDs extracted from its path.
     * Returns them ordered root-first (depth ASC).
     */
    @Query("SELECT c FROM Category c WHERE c.id IN :ids ORDER BY c.depth ASC")
    List<Category> findAllByIdIn(@Param("ids") Collection<Long> ids);

    // ── Sibling-name uniqueness ──────────────────────────────────

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
            FROM Category c
            WHERE c.parent = :parent AND LOWER(c.name) = LOWER(:name)
            """)
    boolean existsByParentAndNameIgnoreCase(@Param("parent") Category parent,
                                            @Param("name") String name);

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
            FROM Category c
            WHERE c.parent IS NULL AND LOWER(c.name) = LOWER(:name)
            """)
    boolean existsByParentNullAndNameIgnoreCase(@Param("name") String name);

    // ── Pagination ────────────────────────────────────────────────

    Page<Category> findAllByActiveTrue(Pageable pageable);

    // ── Admin search / filter ────────────────────────────────────

    @Query("""
            SELECT c FROM Category c
            WHERE (:q IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:depth IS NULL OR c.depth = :depth)
              AND (:nodeType IS NULL OR c.nodeType = :nodeType)
              AND (:active IS NULL OR c.active = :active)
            ORDER BY c.path
            """)
    Page<Category> search(@Param("q") String q,
                          @Param("depth") Integer depth,
                          @Param("nodeType") NodeType nodeType,
                          @Param("active") Boolean active,
                          Pageable pageable);
}
