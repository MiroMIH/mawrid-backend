package com.mawrid.scoring;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScoringConfigRepository extends JpaRepository<ScoringConfig, Long> {

    Optional<ScoringConfig> findBySectorId(Long sectorId);
}
