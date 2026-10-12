package com.realtime.delivery.delivery.dto;

import com.realtime.delivery.delivery.model.AssignmentStatus;
import com.realtime.delivery.delivery.model.DeliveryAssignment;
import com.realtime.delivery.delivery.model.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAssignmentResponse {
    private String id;
    private String orderId;
    private String deliveryPartnerId;
    private String pickupAddress;
    private String dropAddress;
    private AssignmentStatus status;
    private DeliveryStatus deliveryStatus;

    public static DeliveryAssignmentResponse from(DeliveryAssignment a) {
        return new DeliveryAssignmentResponse(a.getId(), a.getOrderId(), a.getDeliveryPartnerId(),
                a.getPickupAddress(), a.getDropAddress(), a.getStatus(), a.getDeliveryStatus());
    }
}
