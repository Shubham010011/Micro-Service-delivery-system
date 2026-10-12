package com.realtime.delivery.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLoginRequest {
    /** The Google ID token (JWT) obtained client-side from Google Identity Services. */
    @NotBlank
    private String idToken;

    /** Required only the first time (account creation): CUSTOMER or DELIVERY_PARTNER. */
    private String role;

    private String phoneNumber;
}
