package com.realtime.delivery.order.model;

public enum OrderStatus {
    PLACED,             // customer just created the order
    LOOKING_FOR_PARTNER,// broadcast sent to available delivery partners
    ASSIGNED,           // a delivery partner has been assigned (auto or by admin) but not yet accepted
    ACCEPTED,           // delivery partner accepted the order
    REJECTED,           // delivery partner rejected -> goes back to LOOKING_FOR_PARTNER
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED
}
