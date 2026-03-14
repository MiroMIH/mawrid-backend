package com.mawrid.notification;

import com.mawrid.common.enums.NotifChannel;
import com.mawrid.common.enums.NotifType;
import com.mawrid.demande.Demande;
import com.mawrid.scoring.DemandeSupplierScore;
import com.mawrid.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    private static final int TIER_IMMEDIATE  = 80;
    private static final int TIER_DELAYED15M = 50;
    private static final int TIER_DELAYED1H  = 30;

    /**
     * Schedule notifications for all scored supplier-demande pairs.
     * Tiers: score>=80 → immediate, 50-79 → +15min, 30-49 → +1h, <30 → in-app only.
     */
    @Async("taskExecutor")
    @Transactional
    public void scheduleNotifications(Demande demande, List<DemandeSupplierScore> scores) {
        LocalDateTime now = LocalDateTime.now();

        for (DemandeSupplierScore score : scores) {
            int finalScore = score.getFinalScore();
            User supplier  = score.getSupplier();

            if (finalScore >= TIER_IMMEDIATE) {
                createNotification(supplier, demande, now, NotifChannel.PUSH);
            } else if (finalScore >= TIER_DELAYED15M) {
                createNotification(supplier, demande, now.plusMinutes(15), NotifChannel.PUSH);
            } else if (finalScore >= TIER_DELAYED1H) {
                createNotification(supplier, demande, now.plusHours(1), NotifChannel.PUSH);
            } else {
                // In-app feed only — still record for tracking
                createNotification(supplier, demande, now, NotifChannel.IN_APP);
            }
        }

        log.info("Scheduled {} notifications for demande '{}'", scores.size(), demande.getTitle());
    }

    private void createNotification(User user, Demande demande,
                                    LocalDateTime scheduledAt, NotifChannel channel) {
        notificationRepository.save(Notification.builder()
                .user(user)
                .demande(demande)
                .type(NotifType.NEW_DEMANDE)
                .channel(channel)
                .scheduledAt(scheduledAt)
                .sent(false)
                .build());
    }

    // ── Legacy helper (kept for compatibility) ───────────────────
    @Async("taskExecutor")
    @Transactional(readOnly = true)
    public void notifySuppliers(Demande demande) {
        log.debug("notifySuppliers called — delegate to scheduleNotifications flow");
    }
}
