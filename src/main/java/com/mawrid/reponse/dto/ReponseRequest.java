package com.mawrid.reponse.dto;

import com.mawrid.reponse.ReponseStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReponseRequest {

    @NotNull(message = "Status is required (DISPONIBLE or INDISPONIBLE)")
    private ReponseStatus status;

    private String note;
}
