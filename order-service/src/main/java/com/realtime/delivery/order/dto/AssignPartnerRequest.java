package com.realtime.delivery.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignPartnerRequest {
    @NotBlank
    private String deliveryPartnerId;
}
