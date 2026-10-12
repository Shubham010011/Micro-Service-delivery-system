package com.realtime.delivery.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank
    private String name;

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank
    private String phoneNumber;

    /** Only CUSTOMER or DELIVERY_PARTNER may self-register. ADMIN accounts are seeded/created by another admin. */
    @NotBlank
    private String role;
}
