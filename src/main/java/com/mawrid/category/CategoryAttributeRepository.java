package com.mawrid.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryAttributeRepository extends JpaRepository<CategoryAttribute, Long> {

    List<CategoryAttribute> findByCategoryOrderByDisplayOrderAsc(Category category);

    /**
     * Returns all attributes for a category including inherited ones from ancestors.
     * Ancestors are identified via the materialized path.
     */
    @Query("""
            SELECT a FROM CategoryAttribute a
            WHERE a.category.id IN (
                SELECT c.id FROM Category c
                WHERE :categoryPath LIKE CONCAT(c.path, '%')
            )
            ORDER BY a.category.depth ASC, a.displayOrder ASC
            """)
    List<CategoryAttribute> findAllEffectiveForCategory(@Param("categoryPath") String categoryPath);
}
