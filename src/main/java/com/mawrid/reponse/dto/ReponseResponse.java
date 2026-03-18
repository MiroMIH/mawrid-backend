package com.mawrid.reponse.dto;

import com.mawrid.reponse.ReponseStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class ReponseResponse {
    UUID id;
    UUID demandeId;
    String demandeTitle;
    UUID supplierId;
    String supplierEmail;
    String supplierFirstName;
    String supplierLastName;
    String supplierPhone;
    String supplierCompanyName;
    String supplierWilaya;
    Integer supplierScore;
    ReponseStatus status;
    String note;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
