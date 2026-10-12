package com.realtime.delivery.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BroadcastOrderRequest {
    @NotBlank
    private String orderId;
    private String pickupAddress;
    private String dropAddress;
    private String preferredDeliveryPartnerId;
}
