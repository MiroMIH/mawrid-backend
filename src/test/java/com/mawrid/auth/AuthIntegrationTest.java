package com.mawrid.auth;

import com.mawrid.AbstractIntegrationTest;
import com.mawrid.auth.dto.AuthResponse;
import com.mawrid.auth.dto.LoginRequest;
import com.mawrid.auth.dto.RegisterRequest;
import com.mawrid.common.response.ApiResponse;
import com.mawrid.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void registerAndLoginBuyer() {
        // Register
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("buyer@test.com");
        reg.setPassword("password123");
        reg.setFirstName("Ahmed");
        reg.setLastName("Test");
        reg.setRole(Role.BUYER);

        ResponseEntity<ApiResponse<AuthResponse>> registerResp = rest.exchange(
                "/api/v1/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(reg),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(registerResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResp.getBody()).isNotNull();
        assertThat(registerResp.getBody().getData().getAccessToken()).isNotBlank();

        // Login
        LoginRequest login = new LoginRequest();
        login.setEmail("buyer@test.com");
        login.setPassword("password123");

        ResponseEntity<ApiResponse<AuthResponse>> loginResp = rest.exchange(
                "/api/v1/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(login),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResp.getBody().getData().getRole()).isEqualTo(Role.BUYER);
    }

    @Test
    void registerDuplicateEmailReturnsConflict() {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("dup@test.com");
        reg.setPassword("password123");
        reg.setFirstName("A");
        reg.setLastName("B");
        reg.setRole(Role.SUPPLIER);

        rest.postForEntity("/api/v1/auth/register", reg, Object.class);

        ResponseEntity<Object> second = rest.postForEntity("/api/v1/auth/register", reg, Object.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void registerAsAdminIsForbidden() {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("admin@test.com");
        reg.setPassword("password123");
        reg.setFirstName("A");
        reg.setLastName("B");
        reg.setRole(Role.ADMIN);

        ResponseEntity<Object> resp = rest.postForEntity("/api/v1/auth/register", reg, Object.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
