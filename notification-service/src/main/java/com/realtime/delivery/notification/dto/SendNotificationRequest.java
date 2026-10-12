package com.realtime.delivery.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class SendNotificationRequest {
    @NotBlank
    private String userId;
    @NotBlank
    private String type;
    @NotBlank
    private String title;
    private String message;
    private Map<String, Object> payload;
}
