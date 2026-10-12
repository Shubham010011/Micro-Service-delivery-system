package com.realtime.delivery.auth.service;

import com.realtime.delivery.auth.dto.*;
import com.realtime.delivery.auth.exception.AuthException;
import com.realtime.delivery.auth.model.User;
import com.realtime.delivery.auth.repository.UserRepository;
import com.realtime.delivery.common.constants.Role;
import com.realtime.delivery.common.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final GoogleTokenVerifierService googleTokenVerifierService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw AuthException.conflict("An account with this email already exists");
        }

        Role role = parseSelfRegisterRole(request.getRole());

        // Password is hashed with BCrypt before it is ever stored — the plaintext is discarded immediately.
        String hashed = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .name(request.getName())
                .email(email)
                .passwordHash(hashed)
                .phoneNumber(request.getPhoneNumber())
                .role(role)
                .build();

        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> AuthException.unauthorized("Invalid email or password"));

        if (user.getPasswordHash() == null) {
            throw AuthException.unauthorized("This account uses Google Sign-In. Please continue with Google.");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw AuthException.unauthorized("Invalid email or password");
        }
        if (!user.isActive()) {
            throw AuthException.forbidden("This account has been disabled");
        }
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        GoogleTokenVerifierService.GoogleUser googleUser = googleTokenVerifierService.verify(request.getIdToken());

        User user = userRepository.findByGoogleId(googleUser.googleId())
                .or(() -> userRepository.findByEmail(googleUser.email().toLowerCase()))
                .orElse(null);

        if (user == null) {
            // First time signing in with Google - create the account now.
            Role role = parseSelfRegisterRole(request.getRole() == null || request.getRole().isBlank() ? "CUSTOMER" : request.getRole());
            user = User.builder()
                    .name(googleUser.name())
                    .email(googleUser.email().toLowerCase())
                    .googleId(googleUser.googleId())
                    .phoneNumber(request.getPhoneNumber())
                    .role(role)
                    .build();
            user = userRepository.save(user);
        } else if (user.getGoogleId() == null) {
            // Existing password-based account signing in with Google for the first time - link it.
            user.setGoogleId(googleUser.googleId());
            user = userRepository.save(user);
        }

        if (!user.isActive()) {
            throw AuthException.forbidden("This account has been disabled");
        }
        return buildAuthResponse(user);
    }

    private Role parseSelfRegisterRole(String raw) {
        Role role;
        try {
            role = Role.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw AuthException.badRequest("Role must be CUSTOMER or DELIVERY_PARTNER");
        }
        if (role == Role.ADMIN) {
            throw AuthException.forbidden("Admin accounts cannot be self-registered");
        }
        return role;
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole().name());
    }
}
