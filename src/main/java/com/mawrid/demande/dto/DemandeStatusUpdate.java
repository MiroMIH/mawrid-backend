package com.mawrid.demande.dto;

import com.mawrid.demande.DemandeStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DemandeStatusUpdate {

    @NotNull(message = "Status is required")
    private DemandeStatus status;
}
