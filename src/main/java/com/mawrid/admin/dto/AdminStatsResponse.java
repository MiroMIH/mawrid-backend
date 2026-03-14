package com.mawrid.admin.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminStatsResponse {
    long totalUsers;
    long totalBuyers;
    long totalSuppliers;
    long totalDemandes;
    long openDemandes;
    long totalReponses;
    double responseRate;
}
