package com.mawrid.scoring;

import com.mawrid.demande.Demande;
import com.mawrid.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "demande_supplier_scores")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeSupplierScore {

    @EmbeddedId
    private DemandeSupplierScoreId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("demandeId")
    @JoinColumn(name = "demande_id")
    private Demande demande;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("supplierId")
    @JoinColumn(name = "supplier_id")
    private User supplier;

    @Builder.Default
    private int categoryScore = 0;

    @Builder.Default
    private int proximityScore = 0;

    @Builder.Default
    private int urgencyScore = 0;

    @Builder.Default
    private int buyerScore = 0;

    @Builder.Default
    private int quantityScore = 0;

    /** Sum of all component scores */
    @Builder.Default
    private int baseScore = 0;

    /** Decays over time: 1.00 → 0.20 */
    @Column(precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal decayFactor = BigDecimal.ONE;

    /** finalScore = baseScore × decayFactor (updated nightly) */
    @Builder.Default
    private int finalScore = 0;

    private LocalDateTime scoredAt;
    private LocalDateTime lastDecayAt;
}
