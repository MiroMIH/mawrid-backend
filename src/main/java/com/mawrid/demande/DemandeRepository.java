package com.mawrid.demande;

import com.mawrid.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DemandeRepository extends JpaRepository<Demande, UUID> {

    Page<Demande> findByBuyer(User buyer, Pageable pageable);

    Page<Demande> findByBuyerAndStatus(User buyer, DemandeStatus status, Pageable pageable);

    Page<Demande> findByStatus(DemandeStatus status, Pageable pageable);

    boolean existsByBuyerAndCategoryIdAndStatus(User buyer, Long categoryId, DemandeStatus status);

    /** Demandes that passed their deadline and are still OPEN */
    @Query("SELECT d FROM Demande d WHERE d.status = 'OPEN' AND d.deadline < :today")
    List<Demande> findExpiredOpen(@Param("today") LocalDate today);

    long countByStatus(DemandeStatus status);
}
