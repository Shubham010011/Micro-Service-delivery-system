package com.realtime.delivery.order.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryServiceClient {

    private final RestClient.Builder restClientBuilder;
    private static final String BASE_URL = "http://delivery-service";

    /** Tells delivery-service a brand new order needs a rider - it will broadcast to available partners (or the preferred one first). */
    public void broadcastNewOrder(String orderId, String pickupAddress, String dropAddress, String preferredDeliveryPartnerId) {
        try {
            restClientBuilder.build().post()
                    .uri(BASE_URL + "/internal/deliveries/broadcast")
                    .body(Map.of(
                            "orderId", orderId,
                            "pickupAddress", pickupAddress,
                            "dropAddress", dropAddress,
                            "preferredDeliveryPartnerId", preferredDeliveryPartnerId == null ? "" : preferredDeliveryPartnerId
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Failed to broadcast order {} to delivery-service: {}", orderId, e.getMessage());
        }
    }

    /** Called when a customer cancels, or admin manually assigns - so delivery-service's pending assignment state stays consistent. */
    public void cancelAssignment(String orderId) {
        try {
            restClientBuilder.build().post()
                    .uri(BASE_URL + "/internal/deliveries/{orderId}/cancel", orderId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Failed to cancel assignment for order {}: {}", orderId, e.getMessage());
        }
    }

    /** Admin manually picked a delivery partner - inform delivery-service so it notifies that rider directly. */
    public void manualAssign(String orderId, String deliveryPartnerId, String pickupAddress, String dropAddress) {
        try {
            restClientBuilder.build().post()
                    .uri(BASE_URL + "/internal/deliveries/{orderId}/manual-assign", orderId)
                    .body(Map.of(
                            "deliveryPartnerId", deliveryPartnerId,
                            "pickupAddress", pickupAddress,
                            "dropAddress", dropAddress
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Failed to manually assign order {} to partner {}: {}", orderId, deliveryPartnerId, e.getMessage());
        }
    }
}
