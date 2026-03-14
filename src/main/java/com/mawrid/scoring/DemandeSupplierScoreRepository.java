package com.mawrid.scoring;

import com.mawrid.demande.Demande;
import com.mawrid.demande.DemandeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DemandeSupplierScoreRepository extends JpaRepository<DemandeSupplierScore, DemandeSupplierScoreId> {

    List<DemandeSupplierScore> findByDemande(Demande demande);

    /** Supplier feed: open demandes ordered by score, excluding already-responded */
    @Query("""
            SELECT s FROM DemandeSupplierScore s
            WHERE s.supplier.id = :supplierId
              AND s.demande.status = :status
              AND NOT EXISTS (
                  SELECT r FROM Reponse r
                  WHERE r.demande = s.demande AND r.supplier.id = :supplierId
              )
            ORDER BY s.finalScore DESC
            """)
    List<DemandeSupplierScore> findSupplierFeed(
            @Param("supplierId") UUID supplierId,
            @Param("status") DemandeStatus status
    );

    /** All open demande scores for a supplier (for recomputation after category update) */
    @Query("""
            SELECT s FROM DemandeSupplierScore s
            WHERE s.supplier.id = :supplierId
              AND s.demande.status = 'OPEN'
            """)
    List<DemandeSupplierScore> findOpenScoresForSupplier(@Param("supplierId") UUID supplierId);

    /** All scores for open demandes — used by nightly decay scheduler */
    @Query("""
            SELECT s FROM DemandeSupplierScore s
            WHERE s.demande.status = 'OPEN'
            """)
    List<DemandeSupplierScore> findAllOpenScores();

    void deleteByDemande(Demande demande);
}
