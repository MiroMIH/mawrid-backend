package com.mawrid.scoring;

import com.mawrid.category.Category;
import com.mawrid.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "scoring_config")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoringConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The top-level sector this config applies to (depth = 0) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private Category sector;

    @Builder.Default private int categoryWeight  = 35;
    @Builder.Default private int proximityWeight = 25;
    @Builder.Default private int urgencyWeight   = 20;
    @Builder.Default private int buyerWeight     = 10;
    @Builder.Default private int quantityWeight  = 10;

    @Builder.Default private int notifThresholdImmediate  = 80;
    @Builder.Default private int notifThresholdDelayed15m = 50;
    @Builder.Default private int notifThresholdDelayed1h  = 30;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;
}
