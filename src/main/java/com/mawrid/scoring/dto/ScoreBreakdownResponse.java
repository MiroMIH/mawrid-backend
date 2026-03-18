package com.mawrid.scoring.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class ScoreBreakdownResponse {
    UUID demandeId;
    UUID supplierId;
    String supplierName;
    String supplierCompany;
    String supplierWilaya;
    Integer categoryScore;
    Integer proximityScore;
    Integer urgencyScore;
    Integer buyerScore;
    Integer quantityScore;
    Integer baseScore;
    Double decayFactor;
    Integer finalScore;
    String notificationTier;
}
