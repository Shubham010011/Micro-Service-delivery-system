package com.realtime.delivery.delivery.dto;

import com.realtime.delivery.delivery.model.PartnerStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AvailabilityRequest {
    @NotNull
    private PartnerStatus status;
    private Double lat;
    private Double lng;
}
