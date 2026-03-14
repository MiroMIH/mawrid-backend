package com.mawrid.scoring;

import com.mawrid.AbstractIntegrationTest;
import com.mawrid.category.Category;
import com.mawrid.category.CategoryRepository;
import com.mawrid.demande.Demande;
import com.mawrid.demande.DemandeRepository;
import com.mawrid.demande.DemandeStatus;
import com.mawrid.user.Role;
import com.mawrid.user.User;
import com.mawrid.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ScoringIntegrationTest extends AbstractIntegrationTest {

    @Autowired DemandeScoreEngine scoreEngine;
    @Autowired UserRepository userRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired DemandeRepository demandeRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void scoreEngineProducesValidScore() {
        Category category = categoryRepository.findByParentIsNull().get(0);

        User supplier = userRepository.save(User.builder()
                .email("supplier.score@test.com")
                .password(passwordEncoder.encode("pass"))
                .firstName("Karim")
                .lastName("Score")
                .role(Role.SUPPLIER)
                .wilaya("16")
                .categories(Set.of(category))
                .build());

        User buyer = userRepository.save(User.builder()
                .email("buyer.score@test.com")
                .password(passwordEncoder.encode("pass"))
                .firstName("Ali")
                .lastName("Buyer")
                .role(Role.BUYER)
                .wilaya("16")
                .build());

        Demande demande = demandeRepository.save(Demande.builder()
                .title("Test Demande")
                .category(category)
                .buyer(buyer)
                .quantity(10)
                .wilaya("16")
                .deadline(LocalDate.now().plusDays(5))
                .status(DemandeStatus.OPEN)
                .build());

        DemandeSupplierScore score = scoreEngine.computeScore(demande, supplier);

        assertThat(score.getBaseScore()).isGreaterThan(0);
        assertThat(score.getBaseScore()).isLessThanOrEqualTo(100);
        assertThat(score.getFinalScore()).isEqualTo(score.getBaseScore()); // decay=1.0 initially
        assertThat(score.getCategoryScore()).isGreaterThan(0);
        assertThat(score.getProximityScore()).isGreaterThan(0); // same wilaya
    }

    @Test
    void decayFactorDegradesOverTime() {
        Category category = categoryRepository.findByParentIsNull().get(0);

        User buyer = userRepository.save(User.builder()
                .email("buyer.decay@test.com")
                .password(passwordEncoder.encode("pass"))
                .firstName("B").lastName("D")
                .role(Role.BUYER).build());

        Demande freshDemande = Demande.builder()
                .title("Fresh")
                .category(category)
                .buyer(buyer)
                .status(DemandeStatus.OPEN)
                .build();
        // Simulate a demande created 10 days ago
        freshDemande.setCreatedAt(java.time.LocalDateTime.now().minusDays(10));

        assertThat(scoreEngine.computeDecayFactor(freshDemande))
                .isEqualByComparingTo(java.math.BigDecimal.valueOf(0.20));
    }
}
