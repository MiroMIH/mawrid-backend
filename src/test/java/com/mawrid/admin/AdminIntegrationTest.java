package com.mawrid.admin;

import com.mawrid.AbstractIntegrationTest;
import com.mawrid.admin.dto.AdminStatsResponse;
import com.mawrid.auth.dto.AuthResponse;
import com.mawrid.auth.dto.LoginRequest;
import com.mawrid.auth.dto.RegisterRequest;
import com.mawrid.common.response.ApiResponse;
import com.mawrid.user.Role;
import com.mawrid.user.User;
import com.mawrid.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class AdminIntegrationTest extends AbstractIntegrationTest {

    @Autowired TestRestTemplate rest;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String adminToken;

    @BeforeEach
    void setUp() {
        // Create an admin directly in DB (self-registration as ADMIN is blocked)
        if (userRepository.findByEmail("admin@mawrid.dz").isEmpty()) {
            userRepository.save(User.builder()
                    .email("admin@mawrid.dz")
                    .password(passwordEncoder.encode("admin123"))
                    .firstName("Admin")
                    .lastName("Mawrid")
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build());
        }

        LoginRequest login = new LoginRequest();
        login.setEmail("admin@mawrid.dz");
        login.setPassword("admin123");

        ResponseEntity<ApiResponse<AuthResponse>> resp = rest.exchange(
                "/api/v1/auth/login", HttpMethod.POST,
                new HttpEntity<>(login),
                new ParameterizedTypeReference<>() {}
        );
        adminToken = resp.getBody().getData().getAccessToken();
    }

    @Test
    void adminCanGetStats() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(adminToken);

        ResponseEntity<ApiResponse<AdminStatsResponse>> resp = rest.exchange(
                "/api/v1/admin/stats", HttpMethod.GET,
                new HttpEntity<>(h),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getData().getTotalUsers()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void nonAdminCannotAccessAdminEndpoints() {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("buyer.admin@test.com");
        reg.setPassword("password123");
        reg.setFirstName("B"); reg.setLastName("T");
        reg.setRole(Role.BUYER);
        rest.postForEntity("/api/v1/auth/register", reg, Object.class);

        LoginRequest login = new LoginRequest();
        login.setEmail("buyer.admin@test.com");
        login.setPassword("password123");
        ResponseEntity<ApiResponse<AuthResponse>> loginResp = rest.exchange(
                "/api/v1/auth/login", HttpMethod.POST,
                new HttpEntity<>(login),
                new ParameterizedTypeReference<>() {}
        );
        String buyerToken = loginResp.getBody().getData().getAccessToken();

        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(buyerToken);

        ResponseEntity<Object> resp = rest.exchange(
                "/api/v1/admin/stats", HttpMethod.GET,
                new HttpEntity<>(h), Object.class
        );

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
