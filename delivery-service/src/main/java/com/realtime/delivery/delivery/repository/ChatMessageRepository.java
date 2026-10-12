package com.realtime.delivery.delivery.repository;

import com.realtime.delivery.delivery.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    List<ChatMessage> findAllByOrderIdOrderBySentAtAsc(String orderId);
}
