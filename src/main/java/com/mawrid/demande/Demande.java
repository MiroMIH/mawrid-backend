package com.mawrid.demande;

import com.mawrid.category.Category;
import com.mawrid.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "demandes")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Demande {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer quantity;
    private String unit;
    private LocalDate deadline;

    /** Buyer's wilaya — used for proximity scoring */
    private String wilaya;

    /** Quality score computed at creation time (0-100) */
    @Builder.Default
    private int qualityScore = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DemandeStatus status = DemandeStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @Column(length = 500)
    private String attachmentUrl;

    @OneToMany(mappedBy = "demande", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DemandeAttribute> attributes = new ArrayList<>();

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Demande d)) return false;
        return id != null && id.equals(d.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
