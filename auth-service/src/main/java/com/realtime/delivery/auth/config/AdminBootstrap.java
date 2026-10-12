package com.realtime.delivery.auth.config;

import com.realtime.delivery.auth.model.User;
import com.realtime.delivery.auth.repository.UserRepository;
import com.realtime.delivery.common.constants.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first ADMIN account on startup when ADMIN_EMAIL and ADMIN_PASSWORD are set (admins can't
 * self-register). Does nothing if either is unset or an account with that email already exists, so it is
 * safe to leave configured. Remove the variables again once you have logged in and rotated the password.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${bootstrap.admin.email:}")
    private String adminEmail;

    @Value("${bootstrap.admin.password:}")
    private String adminPassword;

    @Value("${bootstrap.admin.phone:+10000000000}")
    private String adminPhone;

    @Override
    public void run(String... args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) return;
        String email = adminEmail.trim().toLowerCase();
        if (userRepository.existsByEmail(email)) return;
        if (adminPassword.contains("CHANGE_ME")) {
            log.warn("ADMIN_PASSWORD is still the placeholder from .env.example - admin account NOT created");
            return;
        }
        if (adminPassword.length() < 8) {
            log.warn("ADMIN_PASSWORD is shorter than 8 characters - admin account NOT created");
            return;
        }
        userRepository.save(User.builder()
                .name("Administrator")
                .email(email)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .phoneNumber(adminPhone)
                .role(Role.ADMIN)
                .build());
        log.info("Bootstrap admin account created for {}", email);
    }
}
