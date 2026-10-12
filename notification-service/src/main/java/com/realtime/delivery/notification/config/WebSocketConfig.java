package com.realtime.delivery.notification.config;

import com.realtime.delivery.common.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Client connects with ?token=<jwt> (via the gateway's /ws/notifications route) then subscribes to /user/queue/notifications.
        registry.addEndpoint("/ws/notifications")
                .setAllowedOriginPatterns("*")
                .addInterceptors(authHandshakeInterceptor())
                .setHandshakeHandler(principalHandshakeHandler())
                .withSockJS();
    }

    @Bean
    public DefaultHandshakeHandler principalHandshakeHandler() {
        return new DefaultHandshakeHandler() {
            @Override
            protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
                String userId = (String) attributes.get("userId");
                return userId == null ? super.determineUser(request, wsHandler, attributes) : () -> userId;
            }
        };
    }

    @Bean
    public HandshakeInterceptor authHandshakeInterceptor() {
        JwtUtil jwtUtil = new JwtUtil(jwtSecret, 86400000L);
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                            WebSocketHandler wsHandler, Map<String, Object> attributes) {
                String token = null;
                if (request instanceof ServletServerHttpRequest servletRequest) {
                    HttpServletRequest httpReq = servletRequest.getServletRequest();
                    token = httpReq.getParameter("token");
                }
                if (token == null || !jwtUtil.isValid(token)) {
                    response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                    return false;
                }
                attributes.put("userId", jwtUtil.extractUserId(token));
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                        WebSocketHandler wsHandler, Exception exception) {
                // no-op
            }
        };
    }
}
