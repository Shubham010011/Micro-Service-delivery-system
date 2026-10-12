package com.realtime.delivery.delivery.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "delivery_partner_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryPartnerProfile {

    /** Same id as the User entity in auth-service (the "sub" claim of the JWT). */
    @Id
    private String partnerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PartnerStatus status = PartnerStatus.OFFLINE;

    @Column(name = "current_order_id")
    private String currentOrderId;

    @Column(name = "last_lat")
    private Double lastLat;

    @Column(name = "last_lng")
    private Double lastLng;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }
}
