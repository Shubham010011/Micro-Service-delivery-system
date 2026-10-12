package com.realtime.delivery.notification.controller;

import com.realtime.delivery.common.dto.ApiResponse;
import com.realtime.delivery.notification.dto.NotificationResponse;
import com.realtime.delivery.notification.dto.SendNotificationRequest;
import com.realtime.delivery.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Called service-to-service by order-service (new order / status change) and
 * delivery-service (new offer to a driver and chat message alert).
 * Never routed through the API Gateway.
 */
@RestController
@RequestMapping("/internal/notifications")
@RequiredArgsConstructor
public class InternalNotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    public ApiResponse<NotificationResponse> send(@Valid @RequestBody SendNotificationRequest request) {
        return ApiResponse.ok(notificationService.send(request));
    }
}
