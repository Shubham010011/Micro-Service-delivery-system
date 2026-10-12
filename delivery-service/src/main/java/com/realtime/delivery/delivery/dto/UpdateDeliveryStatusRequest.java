package com.realtime.delivery.delivery.dto;

import com.realtime.delivery.delivery.model.DeliveryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateDeliveryStatusRequest {
    @NotNull
    private DeliveryStatus status;
}
