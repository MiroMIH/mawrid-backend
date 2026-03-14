package com.mawrid.scoring;

import com.mawrid.demande.Demande;
import com.mawrid.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Pure scoring engine — no side effects, returns a computed DemandeSupplierScore.
 * Called on demande creation for every eligible supplier.
 */
@Service
@RequiredArgsConstructor
public class DemandeScoreEngine {

    private final ScoringConfigRepository scoringConfigRepository;

    public DemandeSupplierScore computeScore(Demande demande, User supplier) {
        ScoringConfig config = resolveConfig(demande);

        int categoryScore  = computeCategoryScore(demande, supplier, config);
        int proximityScore = computeProximityScore(demande, supplier, config);
        int urgencyScore   = computeUrgencyScore(demande, config);
        int buyerScore     = computeBuyerScore(demande, config);
        int quantityScore  = computeQuantityScore(demande, config);

        int baseScore  = categoryScore + proximityScore + urgencyScore + buyerScore + quantityScore;
        int finalScore = baseScore; // decayFactor starts at 1.0

        return DemandeSupplierScore.builder()
                .id(new DemandeSupplierScoreId(demande.getId(), supplier.getId()))
                .demande(demande)
                .supplier(supplier)
                .categoryScore(categoryScore)
                .proximityScore(proximityScore)
                .urgencyScore(urgencyScore)
                .buyerScore(buyerScore)
                .quantityScore(quantityScore)
                .baseScore(baseScore)
                .decayFactor(BigDecimal.ONE)
                .finalScore(finalScore)
                .scoredAt(LocalDateTime.now())
                .build();
    }

    // ── Component scorers ────────────────────────────────────────

    private int computeCategoryScore(Demande demande, User supplier, ScoringConfig config) {
        String demandePath = demande.getCategory().getPath();
        int maxScore = config.getCategoryWeight();

        int minDepthDiff = supplier.getCategories().stream()
                .filter(c -> demandePath.equals(c.getPath()) || demandePath.startsWith(c.getPath() + "."))
                .mapToInt(c -> {
                    String[] demandeParts   = demandePath.split("\\.");
                    String[] supplierParts  = c.getPath().split("\\.");
                    return demandeParts.length - supplierParts.length;
                })
                .min()
                .orElse(99);

        return switch (minDepthDiff) {
            case 0  -> maxScore;                          // exact match
            case 1  -> (int) (maxScore * 0.71);           // parent
            case 2  -> (int) (maxScore * 0.43);           // grandparent
            default -> (int) (maxScore * 0.29);           // higher ancestor
        };
    }

    private int computeProximityScore(Demande demande, User supplier, ScoringConfig config) {
        int maxScore = config.getProximityWeight();
        if (demande.getWilaya() == null || supplier.getWilaya() == null) {
            return (int) (maxScore * 0.20); // anywhere in Algeria
        }
        if (demande.getWilaya().equals(supplier.getWilaya())) {
            return maxScore; // same wilaya
        }
        return (int) (maxScore * 0.48); // different wilaya — simplified
    }

    private int computeUrgencyScore(Demande demande, ScoringConfig config) {
        int maxScore = config.getUrgencyWeight();
        if (demande.getDeadline() == null) return (int) (maxScore * 0.25);

        long daysUntilDeadline = ChronoUnit.DAYS.between(LocalDate.now(), demande.getDeadline());

        if (daysUntilDeadline < 3)  return maxScore;
        if (daysUntilDeadline < 7)  return (int) (maxScore * 0.75);
        if (daysUntilDeadline < 14) return (int) (maxScore * 0.50);
        return (int) (maxScore * 0.25);
    }

    private int computeBuyerScore(Demande demande, ScoringConfig config) {
        // MVP: neutral buyer score — placeholder for future reputation tracking
        return (int) (config.getBuyerWeight() * 0.6);
    }

    private int computeQuantityScore(Demande demande, ScoringConfig config) {
        // MVP: give full score if quantity is specified
        if (demande.getQuantity() != null && demande.getQuantity() > 0) {
            return config.getQuantityWeight();
        }
        return (int) (config.getQuantityWeight() * 0.5);
    }

    // ── Decay ────────────────────────────────────────────────────

    public BigDecimal computeDecayFactor(Demande demande) {
        if (demande.getCreatedAt() == null) return BigDecimal.ONE;
        long days = ChronoUnit.DAYS.between(demande.getCreatedAt().toLocalDate(), LocalDate.now());

        if (days <= 0)  return BigDecimal.valueOf(1.00);
        if (days <= 2)  return BigDecimal.valueOf(0.85);
        if (days <= 5)  return BigDecimal.valueOf(0.70);
        if (days <= 7)  return BigDecimal.valueOf(0.50);
        return BigDecimal.valueOf(0.20);
    }

    // ── Config resolution ────────────────────────────────────────

    private ScoringConfig resolveConfig(Demande demande) {
        // Find the root sector (depth 0) of this category's path
        String path = demande.getCategory().getPath();
        String rootId = path.split("\\.")[0];
        try {
            Long sectorId = Long.parseLong(rootId);
            return scoringConfigRepository.findBySectorId(sectorId)
                    .orElseGet(this::defaultConfig);
        } catch (NumberFormatException e) {
            return defaultConfig();
        }
    }

    private ScoringConfig defaultConfig() {
        return ScoringConfig.builder().build();
    }
}
