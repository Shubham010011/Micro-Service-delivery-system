package com.realtime.delivery.common.constants;

/**
 * Headers set by the API Gateway after it validates the JWT, and trusted
 * by internal services. In Kubernetes, downstream services are NOT exposed
 * via the Ingress — only api-gateway is — so these headers cannot be forged
 * by an external client. Each service ALSO re-validates the raw JWT
 * (defense in depth) via {@code JwtUtil}.
 */
public final class Headers {
    private Headers() {}

    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLE = "X-User-Role";
    public static final String USER_EMAIL = "X-User-Email";
    public static final String AUTHORIZATION = "Authorization";
}
