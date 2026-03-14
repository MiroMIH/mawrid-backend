package com.mawrid.matching;

import com.mawrid.demande.Demande;
import com.mawrid.notification.NotificationService;
import com.mawrid.scoring.DemandeScoreEngine;
import com.mawrid.scoring.DemandeSupplierScore;
import com.mawrid.scoring.DemandeSupplierScoreRepository;
import com.mawrid.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Runs matching + scoring + notification scheduling asynchronously
 * after a demande is saved (called from DemandeService via a different bean
 * to enable Spring AOP @Async proxy).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingOrchestrator {

    private final MatchingService matchingService;
    private final DemandeScoreEngine scoreEngine;
    private final DemandeSupplierScoreRepository scoreRepository;
    private final NotificationService notificationService;

    @Async("taskExecutor")
    @Transactional
    public void run(Demande demande) {
        try {
            List<User> suppliers = matchingService.findEligibleSuppliers(demande);

            List<DemandeSupplierScore> scores = suppliers.stream()
                    .map(s -> scoreEngine.computeScore(demande, s))
                    .toList();

            scoreRepository.saveAll(scores);
            notificationService.scheduleNotifications(demande, scores);

            log.info("Matching complete for demande '{}': {} suppliers scored",
                    demande.getTitle(), scores.size());
        } catch (Exception ex) {
            log.error("Matching/scoring failed for demande {}: {}", demande.getId(), ex.getMessage(), ex);
        }
    }
}
