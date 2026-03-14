package com.mawrid.auth.dto;

import com.mawrid.user.Role;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class AuthResponse {
    String accessToken;
    String refreshToken;
    String tokenType;
    UUID userId;
    String email;
    Role role;
}
