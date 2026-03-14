package com.mawrid.demande;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DemandeAttributeRepository extends JpaRepository<DemandeAttribute, UUID> {

    List<DemandeAttribute> findByDemande(Demande demande);
}
