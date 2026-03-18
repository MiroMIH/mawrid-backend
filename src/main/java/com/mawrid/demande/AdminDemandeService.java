package com.mawrid.demande;

import com.mawrid.scoring.DemandeSupplierScore;
import com.mawrid.scoring.DemandeSupplierScoreRepository;
import com.mawrid.scoring.dto.ScoreBreakdownResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminDemandeService {

    private final DemandeService demandeService;
    private final DemandeRepository demandeRepository;
    private final DemandeSupplierScoreRepository scoreRepository;

    @Transactional(readOnly = true)
    public List<ScoreBreakdownResponse> getScoreBreakdown(UUID demandeId) {
        Demande demande = demandeService.findOrThrow(demandeId);
        List<DemandeSupplierScore> scores = scoreRepository.findByDemande(demande);

        return scores.stream()
                .sorted((a, b) -> Integer.compare(b.getFinalScore(), a.getFinalScore()))
                .map(s -> ScoreBreakdownResponse.builder()
                        .demandeId(demandeId)
                        .supplierId(s.getSupplier().getId())
                        .supplierName(s.getSupplier().getFirstName() + " " + s.getSupplier().getLastName())
                        .supplierCompany(s.getSupplier().getCompanyName())
                        .supplierWilaya(s.getSupplier().getWilaya())
                        .categoryScore(s.getCategoryScore())
                        .proximityScore(s.getProximityScore())
                        .urgencyScore(s.getUrgencyScore())
                        .buyerScore(s.getBuyerScore())
                        .quantityScore(s.getQuantityScore())
                        .baseScore(s.getBaseScore())
                        .decayFactor(s.getDecayFactor().doubleValue())
                        .finalScore(s.getFinalScore())
                        .notificationTier(notifTier(s.getFinalScore()))
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminDemandeStatsResponse getStats() {
        long totalOpen      = demandeRepository.countByStatus(DemandeStatus.OPEN);
        long totalClosed    = demandeRepository.countByStatus(DemandeStatus.CLOSED);
        long totalCancelled = demandeRepository.countByStatus(DemandeStatus.CANCELLED);
        long totalExpired   = demandeRepository.countByStatus(DemandeStatus.EXPIRED);
        long total          = demandeRepository.count();

        return AdminDemandeStatsResponse.builder()
                .totalOpen(totalOpen)
                .totalClosed(totalClosed)
                .totalCancelled(totalCancelled)
                .totalExpired(totalExpired)
                .totalAll(total)
                .build();
    }

    private String notifTier(int score) {
        if (score >= 80) return "IMMEDIATE";
        if (score >= 50) return "DELAYED_15MIN";
        if (score >= 30) return "DELAYED_1H";
        return "FEED_ONLY";
    }
}
