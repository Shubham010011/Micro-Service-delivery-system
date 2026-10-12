package com.realtime.delivery.delivery.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceClient {

    private final RestClient.Builder restClientBuilder;
    private static final String BASE_URL = "http://notification-service";

    public void notifyUser(String userId, String type, String title, String message, Map<String, Object> payload) {
        try {
            restClientBuilder.build().post()
                    .uri(BASE_URL + "/internal/notifications/send")
                    .body(Map.of(
                            "userId", userId,
                            "type", type,
                            "title", title,
                            "message", message,
                            "payload", payload == null ? Map.of() : payload
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Failed to push notification to {}: {}", userId, e.getMessage());
        }
    }
}
