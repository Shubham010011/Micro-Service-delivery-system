package com.realtime.delivery.order.dto;

import com.realtime.delivery.order.model.Order;
import com.realtime.delivery.order.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private String id;
    private String customerId;
    private String pickupAddress;
    private String dropAddress;
    private String itemDescription;
    private Double amount;
    private OrderStatus status;
    private String assignedDeliveryPartnerId;
    private Instant createdAt;
    private Instant updatedAt;

    public static OrderResponse from(Order o) {
        return new OrderResponse(o.getId(), o.getCustomerId(), o.getPickupAddress(), o.getDropAddress(),
                o.getItemDescription(), o.getAmount(), o.getStatus(), o.getAssignedDeliveryPartnerId(),
                o.getCreatedAt(), o.getUpdatedAt());
    }
}
