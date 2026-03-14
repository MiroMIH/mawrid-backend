package com.mawrid.category.dto;

import com.mawrid.common.enums.AttributeType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CategoryAttributeResponse {
    Long id;
    String key;
    String label;
    AttributeType type;
    boolean required;
    boolean inherited;
    int displayOrder;
}
