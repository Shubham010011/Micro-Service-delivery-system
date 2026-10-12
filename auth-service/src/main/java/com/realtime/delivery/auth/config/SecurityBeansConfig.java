package com.realtime.delivery.auth.config;

import com.realtime.delivery.common.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityBeansConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-validity-ms:86400000}")
    private long accessTokenValidityMs;

    /** BCrypt with strength 12 — this is how every password is hashed before it ever touches the DB. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil(jwtSecret, accessTokenValidityMs);
    }
}
