package com.realtime.delivery.delivery.client;

import com.realtime.delivery.delivery.exception.DeliveryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderServiceClient {

    private final RestClient.Builder restClientBuilder;
    private static final String BASE_URL = "http://order-service";

    /**
     * Pushes a status change to order-service. Throws on failure so the caller's DB transaction rolls back
     * instead of leaving delivery-service and order-service silently out of sync.
     */
    public void updateOrderStatus(String orderId, String status, String deliveryPartnerId) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("status", status);
            body.put("deliveryPartnerId", deliveryPartnerId);
            restClientBuilder.build().patch()
                    .uri(BASE_URL + "/internal/orders/{orderId}/status", orderId)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.error("order-service rejected status update of order {} to {}: {} {}", orderId, status, e.getStatusCode(), e.getMessage());
            if (e.getStatusCode().value() == 404) throw DeliveryException.notFound("Order not found");
            if (e.getStatusCode().value() == 409) throw DeliveryException.conflict("Order can no longer change status (it may have been cancelled)");
            throw DeliveryException.unavailable("Could not update the order right now. Please try again.");
        } catch (Exception e) {
            log.error("Failed to update order {} status to {}: {}", orderId, status, e.getMessage());
            throw DeliveryException.unavailable("Could not update the order right now. Please try again.");
        }
    }

    /** @throws DeliveryException 404 if the order doesn't exist, 503 if order-service can't be reached. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getOrder(String orderId) {
        try {
            Map<String, Object> resp = restClientBuilder.build().get()
                    .uri(BASE_URL + "/internal/orders/{orderId}", orderId)
                    .retrieve()
                    .body(Map.class);
            Map<String, Object> data = resp == null ? null : (Map<String, Object>) resp.get("data");
            if (data == null) throw DeliveryException.notFound("Order not found");
            return data;
        } catch (DeliveryException e) {
            throw e;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) throw DeliveryException.notFound("Order not found");
            log.error("Failed to fetch order {}: {}", orderId, e.getMessage());
            throw DeliveryException.unavailable("Order service is unavailable. Please try again.");
        } catch (Exception e) {
            log.error("Failed to fetch order {}: {}", orderId, e.getMessage());
            throw DeliveryException.unavailable("Order service is unavailable. Please try again.");
        }
    }
}
