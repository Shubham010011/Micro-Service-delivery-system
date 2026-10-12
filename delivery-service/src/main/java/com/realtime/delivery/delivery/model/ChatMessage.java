package com.realtime.delivery.delivery.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "sender_id", nullable = false)
    private String senderId;

    @Column(name = "sender_role", nullable = false)
    private String senderRole; // CUSTOMER or DELIVERY_PARTNER

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @PrePersist
    void onCreate() {
        this.sentAt = Instant.now();
    }
}
