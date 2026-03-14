package com.mawrid.demande.dto;

import com.mawrid.demande.DemandeStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class DemandeResponse {
    UUID id;
    String title;
    String description;
    Integer quantity;
    String unit;
    LocalDate deadline;
    String wilaya;
    int qualityScore;
    DemandeStatus status;
    Long categoryId;
    String categoryName;
    UUID buyerId;
    String buyerCompanyName;
    String attachmentUrl;
    List<DemandeAttributeDto> attributes;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
