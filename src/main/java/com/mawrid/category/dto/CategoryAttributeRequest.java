package com.mawrid.category.dto;

import com.mawrid.common.enums.AttributeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryAttributeRequest {

    @NotBlank(message = "Key is required")
    @Size(max = 100)
    private String key;

    @NotBlank(message = "Label is required")
    @Size(max = 200)
    private String label;

    @NotNull(message = "Type is required")
    private AttributeType type;

    private boolean required;

    private int displayOrder;
}
