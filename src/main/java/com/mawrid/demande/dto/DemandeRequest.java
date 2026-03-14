package com.mawrid.demande.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class DemandeRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255)
    private String title;

    private String description;

    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    @Size(max = 50)
    private String unit;

    @Future(message = "Deadline must be in the future")
    private LocalDate deadline;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    /** Buyer's wilaya code (1-58) */
    private String wilaya;

    /** Structured attributes — mandatory schema + free custom attributes */
    @Valid
    private List<DemandeAttributeDto> attributes = new ArrayList<>();
}
