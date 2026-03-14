package com.mawrid.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByParentIsNull();

    boolean existsBySlug(String slug);

    Optional<Category> findBySlug(String slug);
}
