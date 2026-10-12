package com.realtime.delivery.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Runs on EVERY request through the gateway, before it is routed to any
 * downstream service. This is the single security choke point of the whole
 * system:
 *
 *  1. Public paths (auth, actuator, websocket handshakes) pass straight through.
 *  2. Everything else MUST carry a valid "Authorization: Bearer <jwt>".
 *  3. Any client-supplied X-User-* header is stripped first, so it can never
 *     be spoofed - only the value THIS filter sets, after verifying the JWT,
 *     reaches a downstream service.
 *  4. Role-based path guarding happens HERE: a CUSTOMER token hitting
 *     /admin/** or /staff/** is rejected with 403 before it ever reaches
 *     order-service/delivery-service. This is how admin and delivery-partner
 *     endpoints stay invisible/unreachable to ordinary customers.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final SecretKey key;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/", "/actuator/", "/ws/", "/eureka"
    );

    /** Same rules as common's JwtSecrets (the gateway is reactive and deliberately does not depend on the servlet-based common module). */
    private static String requireStrongSecret(String secret) {
        if (secret == null || secret.isBlank() || secret.length() < 32
                || secret.contains("CHANGE_ME") || secret.startsWith("c2VjcmV0LWtleS1mb3ItcmVhbHRpbWUt")) {
            throw new IllegalStateException("JWT_SECRET is missing, too short (<32 chars) or still a placeholder. "
                    + "Generate one with: openssl rand -base64 48");
        }
        return secret;
    }

    public JwtAuthenticationFilter(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(requireStrongSecret(secret).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Strip anything the caller tried to set themselves - never trust client-supplied identity headers.
        ServerHttpRequest.Builder mutate = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove("X-User-Id");
                    h.remove("X-User-Role");
                    h.remove("X-User-Email");
                });

        if (isPublic(path)) {
            exchange = exchange.mutate().request(mutate.build()).build();
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Missing or malformed Authorization header");
        }

        String token = authHeader.substring(7);
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }

        String userId = claims.getSubject();
        String role = claims.get("role", String.class);
        String email = claims.get("email", String.class);

        // --- Role-based path guarding: this is what keeps /admin and /staff invisible to customers ---
        if (path.startsWith("/admin/") && !"ADMIN".equals(role)) {
            return reject(exchange, HttpStatus.FORBIDDEN, "Admin access required");
        }
        if (path.startsWith("/staff/") && !"DELIVERY_PARTNER".equals(role)) {
            return reject(exchange, HttpStatus.FORBIDDEN, "Delivery partner access required");
        }
        if (path.startsWith("/api/customer/") && !"CUSTOMER".equals(role)) {
            return reject(exchange, HttpStatus.FORBIDDEN, "Customer access required");
        }

        ServerHttpRequest mutatedRequest = mutate
                .header("X-User-Id", userId)
                .header("X-User-Role", role)
                .header("X-User-Email", email == null ? "" : email)
                .build();

        exchange = exchange.mutate().request(mutatedRequest).build();
        return chain.filter(exchange);
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        byte[] bytes = ("{\"success\":false,\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        var buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
