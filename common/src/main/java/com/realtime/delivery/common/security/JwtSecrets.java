package com.realtime.delivery.common.security;

/**
 * Fails fast at startup if the JWT signing secret is missing, too short, or one of the placeholder values that
 * used to ship in this repository - a publicly known secret would let anyone forge an ADMIN token.
 */
public final class JwtSecrets {

    private static final String LEGACY_DEFAULT_PREFIX = "c2VjcmV0LWtleS1mb3ItcmVhbHRpbWUt";

    private JwtSecrets() {
    }

    public static String requireStrong(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is not set. Generate one with: openssl rand -base64 48");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET is too short (need at least 32 characters). Generate one with: openssl rand -base64 48");
        }
        if (secret.contains("CHANGE_ME") || secret.startsWith(LEGACY_DEFAULT_PREFIX)) {
            throw new IllegalStateException("JWT_SECRET is still a placeholder/default value. Generate a real one with: openssl rand -base64 48");
        }
        return secret;
    }
}
