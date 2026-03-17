package com.mawrid.demande;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DemandeAttributeRepository extends JpaRepository<DemandeAttribute, UUID> {

    List<DemandeAttribute> findByDemande(Demande demande);

    /** Count how many DemandeAttribute records use a given schema key. */
    @Query("SELECT COUNT(da) FROM DemandeAttribute da WHERE da.key = :key")
    long countByKey(@Param("key") String key);
}
