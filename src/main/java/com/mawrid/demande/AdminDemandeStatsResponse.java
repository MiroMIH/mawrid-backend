package com.mawrid.demande;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminDemandeStatsResponse {
    long totalOpen;
    long totalClosed;
    long totalCancelled;
    long totalExpired;
    long totalAll;
}
