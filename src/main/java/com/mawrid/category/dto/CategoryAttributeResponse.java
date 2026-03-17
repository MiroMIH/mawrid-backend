package com.mawrid.category.dto;

import com.mawrid.common.enums.AttributeType;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CategoryAttributeResponse {
    Long id;
    Long categoryId;
    String key;
    String label;
    AttributeType type;
    boolean required;
    boolean inherited;
    int displayOrder;
    List<String> options;
}
