package com.realtime.delivery.notification.service;

import com.realtime.delivery.notification.dto.NotificationResponse;
import com.realtime.delivery.notification.dto.SendNotificationRequest;
import com.realtime.delivery.notification.model.Notification;
import com.realtime.delivery.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public NotificationResponse send(SendNotificationRequest request) {
        Notification notification = Notification.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .title(request.getTitle())
                .message(request.getMessage())
                .build();
        notification = notificationRepository.save(notification);

        NotificationResponse response = NotificationResponse.from(notification);

        // Real-time push: the client (driver app, customer app, or admin panel) is subscribed to
        // /user/queue/notifications - Spring resolves the destination using the STOMP session's Principal.
        Map<String, Object> pushPayload = new HashMap<>();
        pushPayload.put("notification", response);
        if (request.getPayload() != null) {
            pushPayload.put("data", request.getPayload());
        }
        messagingTemplate.convertAndSendToUser(request.getUserId(), "/queue/notifications", pushPayload);

        return response;
    }

    public List<NotificationResponse> getForUser(String userId) {
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(NotificationResponse::from).toList();
    }
}
