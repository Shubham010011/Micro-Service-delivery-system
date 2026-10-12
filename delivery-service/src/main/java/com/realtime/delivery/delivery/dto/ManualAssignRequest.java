package com.realtime.delivery.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ManualAssignRequest {
    @NotBlank
    private String deliveryPartnerId;
    private String pickupAddress;
    private String dropAddress;
}
