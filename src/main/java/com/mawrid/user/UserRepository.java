package com.mawrid.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findAll(Pageable pageable);

    Page<User> findByRole(Role role, Pageable pageable);

    Page<User> findByEnabled(boolean enabled, Pageable pageable);

    /**
     * Matching query: find active suppliers whose subscribed category path
     * is a prefix of the demande's category path (or an exact match).
     */
    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN u.categories c
            WHERE u.role = 'SUPPLIER'
              AND u.enabled = true
              AND (:demandePath = c.path OR :demandePath LIKE CONCAT(c.path, '.%'))
            """)
    List<User> findEligibleSuppliers(@Param("demandePath") String demandePath);

    @Query("""
            SELECT u FROM User u
            JOIN u.categories c
            WHERE u.role = 'SUPPLIER'
              AND c = :category
              AND u.enabled = true
            """)
    List<User> findActiveSuppliersByCategory(@Param("category") com.mawrid.category.Category category);

    /** Count active suppliers subscribed to a specific category. */
    @Query("""
            SELECT COUNT(DISTINCT u) FROM User u
            JOIN u.categories c
            WHERE c.id = :categoryId
              AND u.role = com.mawrid.user.Role.SUPPLIER
              AND u.enabled = true
            """)
    long countActiveSuppliersForCategory(@Param("categoryId") Long categoryId);

    /** Count active suppliers subscribed to ANY of the given categories (for subtree stats). */
    @Query("""
            SELECT COUNT(DISTINCT u) FROM User u
            JOIN u.categories c
            WHERE c.id IN :categoryIds
              AND u.role = com.mawrid.user.Role.SUPPLIER
              AND u.enabled = true
            """)
    long countActiveSuppliersInCategories(@Param("categoryIds") Collection<Long> categoryIds);

    long countByRole(Role role);
}
