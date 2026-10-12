package com.realtime.delivery.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.realtime.delivery.auth.exception.AuthException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * Verifies the Google-issued ID token that the frontend obtains via
 * Google Identity Services (google.accounts.id.initialize / renderButton).
 * The token's signature, audience (our client ID) and expiry are all
 * checked before we trust the email / sub claims inside it.
 */
@Service
public class GoogleTokenVerifierService {

    @Value("${google.oauth.client-id}")
    private String googleClientId;

    public record GoogleUser(String googleId, String email, String name) {}

    public GoogleUser verify(String idTokenString) {
        if (googleClientId == null || googleClientId.isBlank() || googleClientId.startsWith("REPLACE_WITH")) {
            throw AuthException.unavailable("Google Sign-In is not configured on this server (set GOOGLE_CLIENT_ID)");
        }
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw AuthException.unauthorized("Invalid Google ID token");
            }
            GoogleIdToken.Payload payload = idToken.getPayload();
            // Never trust (or link accounts by) an email address Google itself hasn't verified.
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw AuthException.unauthorized("Google account email is not verified");
            }
            String email = payload.getEmail();
            String googleId = payload.getSubject();
            String name = (String) payload.get("name");
            return new GoogleUser(googleId, email, name != null ? name : email);
        } catch (GeneralSecurityException | java.io.IOException e) {
            throw AuthException.unavailable("Could not verify the Google token right now. Please try again.");
        }
    }
}
