package com.realtime.delivery.common.security;

import com.realtime.delivery.common.constants.Headers;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Runs inside every downstream microservice (order/delivery/notification).
 *
 * Identity is ALWAYS derived from the cryptographically verified
 * "Authorization: Bearer <jwt>" header, which the gateway forwards untouched.
 * The X-User-* headers are deliberately NOT trusted here: if a service port is
 * ever reachable directly (misconfigured compose/k8s, port-forward, ...), a
 * caller could otherwise forge "X-User-Role: ADMIN" and impersonate anyone.
 */
public class GatewayAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public GatewayAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(Headers.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                var claims = jwtUtil.parseAndValidate(token);
                String userId = claims.getSubject();
                String role = claims.get("role", String.class);
                if (userId != null && role != null) {
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                    var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (RuntimeException e) {
                // Invalid / expired / tampered token: leave the context empty -> Spring Security answers 401/403.
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }
}
