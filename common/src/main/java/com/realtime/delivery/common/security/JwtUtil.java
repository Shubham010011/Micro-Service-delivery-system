package com.realtime.delivery.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Central JWT creation / validation logic shared by auth-service (issues
 * tokens) and every other service + the gateway (validates tokens).
 *
 * The signing secret MUST be identical across all services — it is injected
 * via the JWT_SECRET environment variable / Kubernetes secret "jwt-secret".
 */
public class JwtUtil {

    private final SecretKey key;
    private final long accessTokenValidityMs;

    public JwtUtil(String base64Secret, long accessTokenValidityMs) {
        this.key = Keys.hmacShaKeyFor(JwtSecrets.requireStrong(base64Secret).getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityMs = accessTokenValidityMs;
    }

    public String generateToken(String userId, String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("role", role);
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenValidityMs);
        return Jwts.builder()
                .claims(claims)
                .subject(userId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /** @return parsed claims, or throws JwtException if invalid/expired/tampered */
    public Claims parseAndValidate(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseAndValidate(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractUserId(String token) {
        return parseAndValidate(token).getSubject();
    }

    public String extractRole(String token) {
        return parseAndValidate(token).get("role", String.class);
    }

    public String extractEmail(String token) {
        return parseAndValidate(token).get("email", String.class);
    }
}
