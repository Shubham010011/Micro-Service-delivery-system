package com.realtime.delivery.delivery.model;

/** Mirrors the subset of order-service's OrderStatus that delivery-service drives. */
public enum DeliveryStatus {
    ACCEPTED,
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED
}
