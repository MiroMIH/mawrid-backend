package com.mawrid.admin.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class SimulationResult {
    UUID supplierId;
    String supplierEmail;
    String companyName;
    String wilaya;
    int categoryScore;
    int proximityScore;
    int urgencyScore;
    int baseScore;
    int finalScore;
    String notifTier;
}
