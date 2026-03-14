package com.mawrid.user.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class UpdateCategoriesRequest {

    @NotEmpty(message = "At least one category ID is required")
    private List<Long> categoryIds;
}
