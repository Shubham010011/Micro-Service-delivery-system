package com.realtime.delivery.order.dto;

import com.realtime.delivery.order.model.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Used internally by delivery-service to push status changes back into order-service. */
@Data
public class UpdateOrderStatusRequest {
    @NotNull
    private OrderStatus status;

    private String deliveryPartnerId;
}
