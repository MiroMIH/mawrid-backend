package com.mawrid.category;

import com.mawrid.common.enums.AttributeType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "category_attributes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 100)
    private String key;

    @Column(nullable = false, length = 200)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AttributeType type = AttributeType.TEXT;

    @Builder.Default
    private boolean required = false;

    /** True when this attribute is inherited from a parent node */
    @Builder.Default
    private boolean inherited = false;

    @Builder.Default
    private int displayOrder = 0;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CategoryAttribute a)) return false;
        return id != null && id.equals(a.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
