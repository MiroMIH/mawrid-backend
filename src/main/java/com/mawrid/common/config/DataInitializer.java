package com.mawrid.common.config;

import com.mawrid.category.Category;
import com.mawrid.category.CategoryRepository;
import com.mawrid.common.enums.NodeType;
import com.mawrid.user.Role;
import com.mawrid.user.User;
import com.mawrid.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Seeds categories from data/categories.csv and creates the default superadmin
 * user on first boot if they do not already exist.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.superadmin.email:superadmin@mawrid.dz}")
    private String superadminEmail;

    @Value("${app.seed.superadmin.password:SuperAdmin@2026}")
    private String superadminPassword;

    @Value("${app.seed.superadmin.first-name:Super}")
    private String superadminFirstName;

    @Value("${app.seed.superadmin.last-name:Admin}")
    private String superadminLastName;

    @Override
    public void run(ApplicationArguments args) {
        seedCategories();
        seedSuperadmin();
        seedTestUsers();
    }

    // ── Categories ────────────────────────────────────────────────────────────

    private void seedCategories() {
        if (categoryRepository.count() > 0) {
            log.debug("Categories already seeded — skipping");
            return;
        }

        log.info("Seeding categories from data/categories.csv...");

        Map<String, Category> bySlug = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("data/categories.csv").getInputStream(),
                StandardCharsets.UTF_8))) {

            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) { header = false; continue; } // skip header row
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",", 3);
                if (parts.length < 2) continue;

                String name       = parts[0].trim();
                String slug       = parts[1].trim();
                String parentSlug = parts.length == 3 ? parts[2].trim() : "";

                Category saved;
                if (parentSlug.isEmpty()) {
                    saved = saveRoot(name, slug);
                } else {
                    Category parent = bySlug.get(parentSlug);
                    if (parent == null) {
                        log.warn("Parent slug '{}' not found for '{}' — skipping", parentSlug, name);
                        continue;
                    }
                    saved = saveChild(name, slug, parent);
                }
                bySlug.put(slug, saved);
            }

        } catch (Exception e) {
            log.error("Failed to seed categories from CSV", e);
            return;
        }

        log.info("Category seeding complete — {} categories loaded", bySlug.size());
    }

    private Category saveRoot(String name, String slug) {
        Category c = categoryRepository.save(Category.builder()
                .name(name)
                .slug(slug)
                .path("placeholder")
                .depth(0)
                .nodeType(NodeType.SEEDED)
                .build());
        c.setPath(String.valueOf(c.getId()));
        return categoryRepository.save(c);
    }

    private Category saveChild(String name, String slug, Category parent) {
        Category c = categoryRepository.save(Category.builder()
                .name(name)
                .slug(slug)
                .parent(parent)
                .path("placeholder")
                .depth(parent.getDepth() + 1)
                .nodeType(NodeType.SEEDED)
                .build());
        c.setPath(parent.getPath() + "." + c.getId());
        return categoryRepository.save(c);
    }

    // ── Test users ────────────────────────────────────────────────────────────

    private void seedTestUsers() {
        seedUserIfAbsent("buyer@test.com",    "Password1!", "Karim",  "Benali",   "SONATRACH Approvisionnements", "Alger",      Role.BUYER);
        seedUserIfAbsent("buyer2@test.com",   "Password1!", "Amira",  "Cherif",   "COSIDER Construction",         "Annaba",     Role.BUYER);
        seedUserIfAbsent("supplier@test.com", "Password1!", "Rachid", "Mekki",    "El Hidhab Roulements",         "Annaba",     Role.SUPPLIER);
        seedUserIfAbsent("supplier2@test.com","Password1!", "Fatima", "Zouai",    "Mecatech Fournitures",         "Alger",      Role.SUPPLIER);
        seedUserIfAbsent("supplier3@test.com","Password1!", "Hassan", "Boukhari", "TransAlgerie Logistique",      "Oran",       Role.SUPPLIER);
    }

    private void seedUserIfAbsent(String email, String password, String firstName, String lastName,
                                  String company, String wilaya, Role role) {
        String encoded = passwordEncoder.encode(password);
        userRepository.findByEmail(email).ifPresentOrElse(
                existing -> {
                    existing.setPassword(encoded);
                    existing.setEnabled(true);
                    userRepository.save(existing);
                    log.info("Test user password reset: {} ({})", email, role);
                },
                () -> {
                    userRepository.save(User.builder()
                            .email(email)
                            .password(encoded)
                            .firstName(firstName)
                            .lastName(lastName)
                            .companyName(company)
                            .wilaya(wilaya)
                            .role(role)
                            .enabled(true)
                            .build());
                    log.info("Test user created: {} ({})", email, role);
                }
        );
    }

    // ── Superadmin ────────────────────────────────────────────────────────────

    private void seedSuperadmin() {
        if (userRepository.countByRole(Role.SUPERADMIN) > 0) {
            log.debug("Superadmin already exists — skipping");
            return;
        }

        userRepository.save(User.builder()
                .email(superadminEmail)
                .password(passwordEncoder.encode(superadminPassword))
                .firstName(superadminFirstName)
                .lastName(superadminLastName)
                .role(Role.SUPERADMIN)
                .enabled(true)
                .build());

        log.info("Default superadmin created: {}", superadminEmail);
    }
}
