package com.mawrid.category.dto;

import com.mawrid.common.enums.NodeType;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CategoryResponse {
    Long id;
    String name;
    String slug;
    String path;
    int depth;
    NodeType nodeType;
    boolean active;
    long demandeCount;
    Long parentId;
    List<CategoryResponse> children;
}
