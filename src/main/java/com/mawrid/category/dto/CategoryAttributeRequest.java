package com.mawrid.category.dto;

import com.mawrid.common.enums.AttributeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CategoryAttributeRequest {

    @NotBlank(message = "Key is required")
    @Pattern(regexp = "^[a-z0-9_]+$", message = "Key must contain only lowercase letters, digits, and underscores")
    @Size(max = 100)
    private String key;

    @NotBlank(message = "Label is required")
    @Size(max = 200)
    private String label;

    @NotNull(message = "Type is required")
    private AttributeType type;

    private boolean required = false;

    private int displayOrder = 0;

    /** Only valid when type = SELECT. Must have at least 2 items. */
    private List<String> options;
}
