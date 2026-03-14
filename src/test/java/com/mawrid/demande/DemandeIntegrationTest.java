package com.mawrid.demande;

import com.mawrid.AbstractIntegrationTest;
import com.mawrid.auth.dto.AuthResponse;
import com.mawrid.auth.dto.LoginRequest;
import com.mawrid.auth.dto.RegisterRequest;
import com.mawrid.category.CategoryRepository;
import com.mawrid.common.response.ApiResponse;
import com.mawrid.demande.dto.DemandeRequest;
import com.mawrid.demande.dto.DemandeResponse;
import com.mawrid.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DemandeIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private CategoryRepository categoryRepository;

    private String buyerToken;
    private Long categoryId;

    @BeforeEach
    void setUp() {
        // Register a buyer
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("buyer.demande@test.com");
        reg.setPassword("password123");
        reg.setFirstName("Buyer");
        reg.setLastName("Test");
        reg.setRole(Role.BUYER);
        rest.postForEntity("/api/v1/auth/register", reg, Object.class);

        // Login
        LoginRequest login = new LoginRequest();
        login.setEmail("buyer.demande@test.com");
        login.setPassword("password123");
        ResponseEntity<ApiResponse<AuthResponse>> loginResp = rest.exchange(
                "/api/v1/auth/login", HttpMethod.POST,
                new HttpEntity<>(login),
                new ParameterizedTypeReference<>() {}
        );
        buyerToken = loginResp.getBody().getData().getAccessToken();

        // Get first category
        categoryId = categoryRepository.findByParentIsNull().get(0).getId();
    }

    @Test
    void buyerCanCreateDemande() {
        DemandeRequest req = new DemandeRequest();
        req.setTitle("Roulements SKF 6205");
        req.setQuantity(10);
        req.setUnit("pièces");
        req.setDeadline(LocalDate.now().plusDays(7));
        req.setCategoryId(categoryId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(buyerToken);

        ResponseEntity<ApiResponse<DemandeResponse>> resp = rest.exchange(
                "/api/v1/demandes", HttpMethod.POST,
                new HttpEntity<>(req, headers),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().getData().getTitle()).isEqualTo("Roulements SKF 6205");
        assertThat(resp.getBody().getData().getStatus()).isEqualTo(DemandeStatus.OPEN);
    }

    @Test
    void unauthenticatedCannotCreateDemande() {
        DemandeRequest req = new DemandeRequest();
        req.setTitle("Test");
        req.setCategoryId(categoryId);

        ResponseEntity<Object> resp = rest.postForEntity("/api/v1/demandes", req, Object.class);
        assertThat(resp.getStatusCode().value()).isIn(401, 403);
    }
}
