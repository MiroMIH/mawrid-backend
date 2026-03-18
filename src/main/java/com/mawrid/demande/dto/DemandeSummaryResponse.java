package com.mawrid.demande.dto;

import com.mawrid.demande.DemandeStatus;
import com.mawrid.reponse.ReponseStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class DemandeSummaryResponse {
    UUID id;
    String title;
    Integer quantity;
    String unit;
    LocalDate deadline;
    DemandeStatus status;
    Integer qualityScore;
    Long categoryId;
    String categoryName;
    String buyerWilaya;
    Integer totalReponses;
    Integer disponibleCount;
    LocalDateTime createdAt;
    long daysUntilDeadline;
    // Feed-specific fields (null for non-supplier context)
    Integer finalScore;
    Integer categoryScore;
    Integer proximityScore;
    Integer urgencyScore;
    // Supplier's own response if any
    SupplierResponseSummary supplierResponse;

    @Value
    @Builder
    public static class SupplierResponseSummary {
        ReponseStatus status;
        LocalDateTime createdAt;
    }
}
