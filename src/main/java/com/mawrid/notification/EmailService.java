package com.mawrid.notification;

import com.mawrid.demande.Demande;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@mawrid.dz}")
    private String fromAddress;

    @Async("taskExecutor")
    public void sendDemandeNotification(String toEmail, Demande demande) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Nouvelle demande d'approvisionnement: " + demande.getTitle());
            message.setText(buildEmailBody(demande));
            mailSender.send(message);
            log.debug("Email notification sent to {}", toEmail);
        } catch (MailException ex) {
            log.error("Failed to send email to {}: {}", toEmail, ex.getMessage());
        }
    }

    private String buildEmailBody(Demande demande) {
        return String.format("""
                Bonjour,

                Une nouvelle demande d'approvisionnement correspond à votre domaine d'activité.

                Titre       : %s
                Catégorie   : %s
                Quantité    : %s %s
                Date limite : %s

                Connectez-vous à Mawrid pour voir les détails et soumettre votre réponse.

                L'équipe Mawrid
                """,
                demande.getTitle(),
                demande.getCategory().getName(),
                demande.getQuantity() != null ? demande.getQuantity() : "-",
                demande.getUnit() != null ? demande.getUnit() : "",
                demande.getDeadline() != null ? demande.getDeadline().toString() : "Non spécifié"
        );
    }
}
