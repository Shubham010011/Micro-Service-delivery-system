package com.realtime.delivery.notification.dto;

import com.realtime.delivery.notification.model.Notification;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private String id;
    private String type;
    private String title;
    private String message;
    private boolean read;
    private Instant createdAt;

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(), n.isRead(), n.getCreatedAt());
    }
}
