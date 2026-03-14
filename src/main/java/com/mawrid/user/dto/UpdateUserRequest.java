package com.mawrid.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateUserRequest {

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Size(max = 30)
    private String phone;

    @Size(max = 200)
    private String companyName;

    private String wilaya;

    @Size(max = 100)
    private String registreCommerce;

    /** Supplier only — subscribed category IDs */
    private List<Long> categoryIds;
}
