package com.mawrid.user.dto;

import com.mawrid.user.Role;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class UserResponse {
    UUID id;
    String email;
    String firstName;
    String lastName;
    String phone;
    String companyName;
    String wilaya;
    String registreCommerce;
    Role role;
    List<Long> categoryIds;
    boolean enabled;
    LocalDateTime createdAt;
}
