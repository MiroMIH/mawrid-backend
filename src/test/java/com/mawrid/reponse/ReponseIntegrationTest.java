package com.mawrid.reponse;

import com.mawrid.AbstractIntegrationTest;
import com.mawrid.auth.dto.AuthResponse;
import com.mawrid.auth.dto.LoginRequest;
import com.mawrid.auth.dto.RegisterRequest;
import com.mawrid.category.CategoryRepository;
import com.mawrid.common.response.ApiResponse;
import com.mawrid.demande.Demande;
import com.mawrid.demande.DemandeRepository;
import com.mawrid.demande.dto.DemandeRequest;
import com.mawrid.demande.dto.DemandeResponse;
import com.mawrid.reponse.dto.ReponseRequest;
import com.mawrid.reponse.dto.ReponseResponse;
import com.mawrid.scoring.DemandeSupplierScore;
import com.mawrid.scoring.DemandeSupplierScoreId;
import com.mawrid.scoring.DemandeSupplierScoreRepository;
import com.mawrid.user.Role;
import com.mawrid.user.User;
import com.mawrid.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReponseIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DemandeRepository demandeRepository;

    @Autowired
    private DemandeSupplierScoreRepository scoreRepository;

    @Test
    void supplierCanRespondDisponible() {
        long id = categoryRepository.findByParentIsNull().get(0).getId();
        String tag = UUID.randomUUID().toString().substring(0, 8);

        String buyerToken    = registerAndLogin("buyer-" + tag + "@test.com",    Role.BUYER);
        String supplierToken = registerAndLogin("supplier-" + tag + "@test.com", Role.SUPPLIER);
        String demandeId     = createDemande(buyerToken, id);

        // Seed a score record so supplier passes the matching check
        seedScoreRecord(demandeId, getSupplierIdByToken(supplierToken));

        ReponseRequest req = new ReponseRequest();
        req.setStatus(ReponseStatus.DISPONIBLE);
        req.setNote("En stock, livraison 48h");

        ResponseEntity<ApiResponse<ReponseResponse>> resp = rest.exchange(
                "/api/v1/reponses/" + demandeId,
                HttpMethod.POST,
                new HttpEntity<>(req, bearerHeader(supplierToken)),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().getData().getStatus()).isEqualTo(ReponseStatus.DISPONIBLE);
    }

    @Test
    void supplierCannotRespondTwice() {
        long id = categoryRepository.findByParentIsNull().get(1).getId();
        String tag = UUID.randomUUID().toString().substring(0, 8);

        String buyerToken    = registerAndLogin("buyer2-" + tag + "@test.com",    Role.BUYER);
        String supplierToken = registerAndLogin("supplier2-" + tag + "@test.com", Role.SUPPLIER);
        String demandeId     = createDemande(buyerToken, id);

        // Seed a score record so supplier passes the matching check
        seedScoreRecord(demandeId, getSupplierIdByToken(supplierToken));

        ReponseRequest req = new ReponseRequest();
        req.setStatus(ReponseStatus.INDISPONIBLE);

        HttpHeaders h = bearerHeader(supplierToken);
        rest.exchange("/api/v1/reponses/" + demandeId,
                HttpMethod.POST, new HttpEntity<>(req, h), Object.class);

        ResponseEntity<Object> second = rest.exchange(
                "/api/v1/reponses/" + demandeId,
                HttpMethod.POST, new HttpEntity<>(req, h), Object.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ── Helpers ──────────────────────────────────────────────────

    private String registerAndLogin(String email, Role role) {
        RegisterRequest r = new RegisterRequest();
        r.setEmail(email);
        r.setPassword("password123");
        r.setFirstName("Test");
        r.setLastName("User");
        r.setRole(role);
        rest.postForEntity("/api/v1/auth/register", r, Object.class);

        LoginRequest l = new LoginRequest();
        l.setEmail(email);
        l.setPassword("password123");
        ResponseEntity<ApiResponse<AuthResponse>> resp = rest.exchange(
                "/api/v1/auth/login", HttpMethod.POST,
                new HttpEntity<>(l), new ParameterizedTypeReference<>() {}
        );
        return resp.getBody().getData().getAccessToken();
    }

    private String createDemande(String buyerToken, long categoryId) {
        DemandeRequest req = new DemandeRequest();
        req.setTitle("Demande test " + UUID.randomUUID());
        req.setQuantity(5);
        req.setDeadline(LocalDate.now().plusDays(14));
        req.setCategoryId(categoryId);

        ResponseEntity<ApiResponse<DemandeResponse>> dr = rest.exchange(
                "/api/v1/demandes", HttpMethod.POST,
                new HttpEntity<>(req, bearerHeader(buyerToken)),
                new ParameterizedTypeReference<>() {}
        );
        assertThat(dr.getStatusCode().value()).isEqualTo(201);
        return dr.getBody().getData().getId().toString();
    }

    private UUID getSupplierIdByToken(String supplierToken) {
        // Find the supplier by looking up recently created suppliers
        // We use a simple approach: get the user from the UserRepository by recent creation
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.SUPPLIER)
                .max(java.util.Comparator.comparing(User::getCreatedAt))
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("No supplier found"));
    }

    private void seedScoreRecord(String demandeIdStr, UUID supplierId) {
        UUID demandeId = UUID.fromString(demandeIdStr);
        Demande demande = demandeRepository.findById(demandeId)
                .orElseThrow(() -> new IllegalStateException("Demande not found: " + demandeId));
        User supplier = userRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalStateException("Supplier not found: " + supplierId));

        DemandeSupplierScoreId scoreId = new DemandeSupplierScoreId(demandeId, supplierId);
        if (!scoreRepository.existsById(scoreId)) {
            scoreRepository.save(DemandeSupplierScore.builder()
                    .id(scoreId)
                    .demande(demande)
                    .supplier(supplier)
                    .categoryScore(50)
                    .proximityScore(20)
                    .urgencyScore(10)
                    .buyerScore(10)
                    .quantityScore(10)
                    .baseScore(100)
                    .decayFactor(BigDecimal.ONE)
                    .finalScore(100)
                    .scoredAt(LocalDateTime.now())
                    .build());
        }
    }

    private HttpHeaders bearerHeader(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return h;
    }
}
