package com.mawrid.category.dto;

import com.mawrid.common.enums.NodeType;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CategoryTreeResponse {
    Long id;
    String name;
    String slug;
    int depth;
    NodeType nodeType;
    boolean active;
    long demandeCount;
    @Builder.Default
    List<CategoryTreeResponse> children = List.of();
}
