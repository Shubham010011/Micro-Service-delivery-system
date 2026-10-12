package com.realtime.delivery.delivery.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * An order that still needs a delivery partner. Created when the order is first broadcast and removed once a partner
 * accepts (or the order is cancelled / given up on). The dispatch scheduler keeps retrying every row here, so an order
 * placed while nobody is online is picked up as soon as a partner becomes AVAILABLE.
 */
@Entity
@Table(name = "pending_dispatches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingDispatch {

    @Id
    @Column(name = "order_id")
    private String orderId;

    @Column(name = "pickup_address")
    private String pickupAddress;

    @Column(name = "drop_address")
    private String dropAddress;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (lastAttemptAt == null) lastAttemptAt = createdAt;
    }
}
