package com.mawrid.reponse;

import com.mawrid.demande.Demande;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReponseRepository extends JpaRepository<Reponse, UUID> {

    Page<Reponse> findByDemandeAndStatus(Demande demande, ReponseStatus status, Pageable pageable);

    Page<Reponse> findByDemande(Demande demande, Pageable pageable);

    List<Reponse> findByDemande(Demande demande);

    boolean existsByDemandeIdAndSupplierId(UUID demandeId, UUID supplierId);

    Optional<Reponse> findByDemandeIdAndSupplierId(UUID demandeId, UUID supplierId);

    long countByDemandeId(UUID demandeId);

    long countByDemandeIdAndStatus(UUID demandeId, ReponseStatus status);

    Page<Reponse> findBySupplierId(UUID supplierId, Pageable pageable);
}
