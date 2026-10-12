package com.realtime.delivery.notification.repository;

import com.realtime.delivery.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findAllByUserIdOrderByCreatedAtDesc(String userId);
}
