package com.mawrid.demande.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecategorizeRequest {
    @NotNull
    private Long newCategoryId;
}
