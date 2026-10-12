package com.realtime.delivery.order.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "pickup_address", nullable = false)
    private String pickupAddress;

    @Column(name = "drop_address", nullable = false)
    private String dropAddress;

    @Column(name = "item_description")
    private String itemDescription;

    @Column(name = "amount")
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "assigned_delivery_partner_id")
    private String assignedDeliveryPartnerId;

    /** Set true when the customer explicitly requests the same partner as a previous order (Zomato-style "order again with the same rider"). */
    @Column(name = "preferred_partner_requested")
    @Builder.Default
    private boolean preferredPartnerRequested = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.status == null) this.status = OrderStatus.PLACED;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
