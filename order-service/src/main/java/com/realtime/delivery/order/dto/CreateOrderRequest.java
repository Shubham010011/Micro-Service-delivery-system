package com.realtime.delivery.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateOrderRequest {
    @NotBlank
    private String pickupAddress;

    @NotBlank
    private String dropAddress;

    private String itemDescription;

    private Double amount;

    /** Optional: reorder with the same delivery partner as a previous order, Zomato-style. */
    private String preferredDeliveryPartnerId;
}
