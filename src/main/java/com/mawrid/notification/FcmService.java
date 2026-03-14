package com.mawrid.notification;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FcmService {

    public void sendPushNotification(String fcmToken, String title, String body) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase not initialized. Skipping push notification to token: {}...",
                     fcmToken.substring(0, Math.min(10, fcmToken.length())));
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            String messageId = FirebaseMessaging.getInstance().send(message);
            log.debug("FCM message sent successfully. MessageId: {}", messageId);
        } catch (Exception ex) {
            log.error("Failed to send FCM notification: {}", ex.getMessage());
        }
    }
}
