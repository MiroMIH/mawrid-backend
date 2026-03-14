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
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public void run(ApplicationArguments args) {
        seedCategories();
        seedSuperadmin();
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
