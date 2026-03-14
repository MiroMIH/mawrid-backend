package com.mawrid.notification;

import com.mawrid.common.enums.NotifChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDispatchScheduler {

    private final NotificationRepository notificationRepository;
    private final FcmService fcmService;
    private final EmailService emailService;

    /** Poll every 30 seconds for pending notifications whose scheduledAt has arrived */
    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void dispatchPendingNotifications() {
        List<Notification> pending = notificationRepository.findPendingToDispatch(LocalDateTime.now());

        if (pending.isEmpty()) return;

        log.debug("Dispatching {} pending notifications", pending.size());

        for (Notification notif : pending) {
            try {
                dispatch(notif);
                notif.setSent(true);
                notif.setSentAt(LocalDateTime.now());
            } catch (Exception ex) {
                notif.setFailureReason(ex.getMessage());
                log.error("Failed to dispatch notification {}: {}", notif.getId(), ex.getMessage());
            }
        }

        notificationRepository.saveAll(pending);
    }

    private void dispatch(Notification notif) {
        if (notif.getChannel() == NotifChannel.PUSH) {
            String fcmToken = notif.getUser().getFcmToken();
            if (fcmToken != null && !fcmToken.isBlank()) {
                String title = "Nouvelle demande disponible";
                String body  = notif.getDemande() != null ? notif.getDemande().getTitle() : "";
                fcmService.sendPushNotification(fcmToken, title, body);
            }
        } else if (notif.getChannel() == NotifChannel.EMAIL) {
            if (notif.getDemande() != null) {
                emailService.sendDemandeNotification(notif.getUser().getEmail(), notif.getDemande());
            }
        }
        // IN_APP: no external dispatch — supplier sees it in the scored feed
    }
}
