package com.realtime.delivery.delivery.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "delivery_assignments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "delivery_partner_id", nullable = false)
    private String deliveryPartnerId;

    @Column(name = "pickup_address")
    private String pickupAddress;

    @Column(name = "drop_address")
    private String dropAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status")
    private DeliveryStatus deliveryStatus; // only meaningful once status == ACCEPTED

    @Column(name = "offered_at", nullable = false)
    private Instant offeredAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    /** Optimistic lock: a stale concurrent write fails instead of silently overwriting (first-accept-wins safety net). */
    @Version
    private Long version;

    @PrePersist
    void onCreate() {
        this.offeredAt = Instant.now();
        if (this.status == null) this.status = AssignmentStatus.OFFERED;
    }
}
