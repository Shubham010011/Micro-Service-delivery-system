package com.realtime.delivery.delivery.config;

import com.realtime.delivery.common.security.JwtUtil;
import com.realtime.delivery.delivery.client.OrderServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String CHAT_TOPIC_PREFIX = "/topic/chat/";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Autowired
    @Lazy
    private OrderServiceClient orderServiceClient;

    /**
     * The handshake only proves WHO the user is. Without this, any logged-in user could subscribe to
     * /topic/chat/{anyOrderId} and read other people's chats. Only the order's customer and its
     * assigned delivery partner may subscribe, and clients may not publish through the socket at all
     * (messages are sent via the REST API, which does its own participant check).
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null || accessor.getCommand() == null) return message;

                if (StompCommand.SEND.equals(accessor.getCommand())) {
                    throw new MessageDeliveryException("Publishing over the socket is not allowed; use the REST API");
                }
                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    String destination = accessor.getDestination();
                    Map<String, Object> session = accessor.getSessionAttributes();
                    String userId = session == null ? null : (String) session.get("userId");
                    if (userId == null || destination == null || !destination.startsWith(CHAT_TOPIC_PREFIX)) {
                        throw new MessageDeliveryException("Subscription not allowed");
                    }
                    String orderId = destination.substring(CHAT_TOPIC_PREFIX.length());
                    Map<String, Object> order = orderServiceClient.getOrder(orderId);
                    if (!userId.equals(order.get("customerId")) && !userId.equals(order.get("assignedDeliveryPartnerId"))) {
                        throw new MessageDeliveryException("You are not a participant in this order");
                    }
                }
                return message;
            }
        });
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Client connects with ?token=<jwt> (via the gateway's /ws/chat route) - validated below before the handshake completes.
        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*")
                .addInterceptors(authHandshakeInterceptor())
                .withSockJS();
    }

    @Bean
    public HandshakeInterceptor authHandshakeInterceptor() {
        JwtUtil jwtUtil = new JwtUtil(jwtSecret, 86400000L);
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                            WebSocketHandler wsHandler, Map<String, Object> attributes) {
                String query = request.getURI().getQuery();
                String token = extractParam(query, "token");
                if (token == null || !jwtUtil.isValid(token)) {
                    response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                    return false;
                }
                attributes.put("userId", jwtUtil.extractUserId(token));
                attributes.put("role", jwtUtil.extractRole(token));
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                        WebSocketHandler wsHandler, Exception exception) {
                // no-op
            }

            private String extractParam(String query, String key) {
                if (query == null) return null;
                for (String pair : query.split("&")) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2 && kv[0].equals(key)) return kv[1];
                }
                return null;
            }
        };
    }
}
