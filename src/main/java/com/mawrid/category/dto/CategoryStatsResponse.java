package com.mawrid.category.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CategoryStatsResponse {
    Long id;
    String name;
    long totalDemandes;
    long totalDemandesInSubtree;
    long activeSuppliers;
    long totalSuppliersInSubtree;
    int childrenCount;
    int depth;
    boolean hasActiveChildren;
}
