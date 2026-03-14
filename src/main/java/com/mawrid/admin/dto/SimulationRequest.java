package com.mawrid.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SimulationRequest {

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private String wilaya;
    private LocalDate deadline;
    private Integer quantity;
}
