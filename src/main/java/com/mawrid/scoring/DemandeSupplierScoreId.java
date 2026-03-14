package com.mawrid.scoring;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DemandeSupplierScoreId implements Serializable {
    private UUID demandeId;
    private UUID supplierId;
}
