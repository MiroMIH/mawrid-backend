package com.mawrid.notification;

import com.mawrid.common.enums.NotifChannel;
import com.mawrid.common.enums.NotifType;
import com.mawrid.demande.Demande;
import com.mawrid.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id")
    private Demande demande;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotifType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotifChannel channel;

    @Builder.Default
    private boolean sent = false;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    private LocalDateTime sentAt;

    @Column(length = 500)
    private String failureReason;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification n)) return false;
        return id != null && id.equals(n.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
