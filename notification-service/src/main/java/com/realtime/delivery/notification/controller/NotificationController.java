package com.realtime.delivery.notification.controller;

import com.realtime.delivery.common.dto.ApiResponse;
import com.realtime.delivery.notification.dto.NotificationResponse;
import com.realtime.delivery.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Role-agnostic: every authenticated user (customer, delivery partner, admin) can list their own notification history. */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<NotificationResponse>> myNotifications(@AuthenticationPrincipal String userId) {
        return ApiResponse.ok(notificationService.getForUser(userId));
    }
}
