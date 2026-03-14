package com.mawrid.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /** Pending notifications whose scheduled time has arrived */
    @Query("SELECT n FROM Notification n WHERE n.sent = false AND n.scheduledAt <= :now")
    List<Notification> findPendingToDispatch(LocalDateTime now);

    List<Notification> findByUserIdAndSentFalse(UUID userId);
}
