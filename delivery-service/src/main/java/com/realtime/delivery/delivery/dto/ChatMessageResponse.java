package com.realtime.delivery.delivery.dto;

import com.realtime.delivery.delivery.model.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private String id;
    private String orderId;
    private String senderId;
    private String senderRole;
    private String message;
    private Instant sentAt;

    public static ChatMessageResponse from(ChatMessage m) {
        return new ChatMessageResponse(m.getId(), m.getOrderId(), m.getSenderId(), m.getSenderRole(), m.getMessage(), m.getSentAt());
    }
}
