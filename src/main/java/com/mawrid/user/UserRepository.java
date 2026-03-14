package com.mawrid.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     *
     * A supplier subscribed to "1.11.111" matches a demande in "1.11.111.1111"
     * because the demande path starts with the supplier's category path.
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

    long countByRole(Role role);
}
