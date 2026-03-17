package com.mawrid.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryAttributeRepository extends JpaRepository<CategoryAttribute, Long> {

    List<CategoryAttribute> findByCategoryOrderByDisplayOrderAsc(Category category);

    Optional<CategoryAttribute> findByCategoryAndKey(Category category, String key);

    /**
     * All attributes for a category including inherited ones from ancestors.
     * Uses materialized path: any ancestor whose path is a prefix of categoryPath.
     * Ordered by depth ASC then displayOrder ASC so ancestors come first.
     */
    @Query("""
            SELECT a FROM CategoryAttribute a
            WHERE a.category.id IN (
                SELECT c.id FROM Category c
                WHERE :categoryPath = c.path OR :categoryPath LIKE CONCAT(c.path, '.%')
            )
            ORDER BY a.category.depth ASC, a.displayOrder ASC
            """)
    List<CategoryAttribute> findAllEffectiveForCategory(@Param("categoryPath") String categoryPath);

    /**
     * Max displayOrder currently used in a category (0 if no attributes yet).
     */
    @Query("SELECT COALESCE(MAX(a.displayOrder), 0) FROM CategoryAttribute a WHERE a.category = :category")
    int findMaxDisplayOrderByCategory(@Param("category") Category category);

    boolean existsByCategoryAndKey(Category category, String key);
}
