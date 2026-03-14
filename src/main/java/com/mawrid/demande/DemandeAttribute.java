package com.mawrid.demande;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "demande_attributes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id", nullable = false)
    private Demande demande;

    @Column(nullable = false, length = 100)
    private String key;

    @Column(nullable = false, length = 500)
    private String value;

    /** True when the buyer added this key themselves (beyond mandatory schema) */
    @Builder.Default
    private boolean custom = false;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DemandeAttribute a)) return false;
        return id != null && id.equals(a.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
