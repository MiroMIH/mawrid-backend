package com.mawrid.category.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MoveRequest {
    @NotNull(message = "New parent ID is required")
    private Long newParentId;
}
