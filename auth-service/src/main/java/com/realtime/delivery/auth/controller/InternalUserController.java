package com.realtime.delivery.auth.controller;

import com.realtime.delivery.auth.exception.AuthException;
import com.realtime.delivery.auth.model.User;
import com.realtime.delivery.auth.repository.UserRepository;
import com.realtime.delivery.common.constants.Role;
import com.realtime.delivery.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * These "/internal/**" endpoints are deliberately NOT registered as a route
 * in the API Gateway, so they cannot be reached from outside the Kubernetes
 * cluster - only other microservices calling auth-service directly (via its
 * ClusterIP / Eureka-resolved address) can use them. This is how, e.g., the
 */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserRepository userRepository;

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getProfile(@PathVariable String id) {
        User u = userRepository.findById(id).orElseThrow(() -> AuthException.notFound("User not found"));
        return ApiResponse.ok(Map.of(
                "id", u.getId(),
                "name", u.getName(),
                "email", u.getEmail(),
                "role", u.getRole().name()
        ));
    }


    @GetMapping("/by-role/{role}")
    public ApiResponse<List<Map<String, Object>>> listByRole(@PathVariable String role) {
        Role r;
        try {
            r = Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw AuthException.badRequest("Unknown role: " + role);
        }
        List<Map<String, Object>> result = userRepository.findAllByRole(r).stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "name", u.getName(),
                        "email", u.getEmail(),
                        "active", u.isActive()
                ))
                .toList();
        return ApiResponse.ok(result);
    }
}
