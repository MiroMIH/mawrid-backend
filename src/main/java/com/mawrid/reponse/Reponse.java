package com.mawrid.reponse;

import com.mawrid.demande.Demande;
import com.mawrid.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "reponses",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_reponse_demande_supplier",
        columnNames = {"demande_id", "supplier_id"}
    )
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reponse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id", nullable = false)
    private Demande demande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private User supplier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReponseStatus status;

    @Column(columnDefinition = "TEXT")
    private String note;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reponse r)) return false;
        return id != null && id.equals(r.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
