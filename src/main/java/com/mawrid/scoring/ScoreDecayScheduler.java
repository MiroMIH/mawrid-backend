package com.mawrid.scoring;

import com.mawrid.demande.Demande;
import com.mawrid.demande.DemandeRepository;
import com.mawrid.demande.DemandeStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScoreDecayScheduler {

    private final DemandeSupplierScoreRepository scoreRepository;
    private final DemandeRepository demandeRepository;
    private final DemandeScoreEngine scoreEngine;

    /** Runs every night at 02:00 — decays scores and expires past-deadline demandes */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void runNightlyDecay() {
        log.info("Starting nightly score decay...");

        List<DemandeSupplierScore> scores = scoreRepository.findAllOpenScores();
        int updated = 0;

        for (DemandeSupplierScore score : scores) {
            BigDecimal newDecay = scoreEngine.computeDecayFactor(score.getDemande());
            if (newDecay.compareTo(score.getDecayFactor()) != 0) {
                score.setDecayFactor(newDecay);
                score.setFinalScore((int) (score.getBaseScore() * newDecay.doubleValue()));
                score.setLastDecayAt(LocalDateTime.now());
                updated++;
            }
        }

        scoreRepository.saveAll(scores);
        log.info("Nightly decay: updated {} scores", updated);

        expireDeadlinedDemandes();
    }

    private void expireDeadlinedDemandes() {
        List<Demande> expired = demandeRepository.findExpiredOpen(LocalDate.now());
        for (Demande d : expired) {
            d.setStatus(DemandeStatus.EXPIRED);
            d.setExpiredAt(LocalDateTime.now());
            log.info("Auto-expired demande {} (deadline: {})", d.getId(), d.getDeadline());
        }
        demandeRepository.saveAll(expired);
    }
}
