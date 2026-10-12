package com.realtime.delivery.delivery.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthServiceClient {

    private final RestClient.Builder restClientBuilder;
    private static final String BASE_URL = "http://auth-service";

    @SuppressWarnings("unchecked")
    public Map<String, Object> getProfile(String userId) {
        try {
            Map<String, Object> resp = restClientBuilder.build().get()
                    .uri(BASE_URL + "/internal/users/{id}", userId)
                    .retrieve()
                    .body(Map.class);
            return (Map<String, Object>) resp.get("data");
        } catch (Exception e) {
            log.error("Failed to fetch profile for {}: {}", userId, e.getMessage());
            return Map.of();
        }
    }


    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listByRole(String role) {
        try {
            Map<String, Object> resp = restClientBuilder.build().get()
                    .uri(BASE_URL + "/internal/users/by-role/{role}", role)
                    .retrieve()
                    .body(Map.class);
            return (List<Map<String, Object>>) resp.get("data");
        } catch (Exception e) {
            log.error("Failed to list users by role {}: {}", role, e.getMessage());
            return List.of();
        }
    }
}
