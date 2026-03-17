package com.mawrid.category.dto;

import com.mawrid.common.enums.NodeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryCreateRequest {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 200, message = "Name must be between 2 and 200 characters")
    private String name;

    /** Null = root sector */
    private Long parentId;

    /** Defaults to ADMIN_CREATED; LEAF can be set explicitly */
    private NodeType nodeType = NodeType.ADMIN_CREATED;
}
