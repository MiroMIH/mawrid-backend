package com.mawrid.demande.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DemandeAttributeDto {

    @NotBlank(message = "Attribute key is required")
    @Size(max = 100)
    private String key;

    @NotBlank(message = "Attribute value is required")
    @Size(max = 500)
    private String value;

    private boolean custom;
}
