package com.realtime.delivery.delivery.controller;

import com.realtime.delivery.common.dto.ApiResponse;
import com.realtime.delivery.delivery.dto.BroadcastOrderRequest;
import com.realtime.delivery.delivery.dto.ManualAssignRequest;
import com.realtime.delivery.delivery.service.DeliveryAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Not routed through the API Gateway - reachable only from order-service, service-to-service. */
@RestController
@RequestMapping("/internal/deliveries")
@RequiredArgsConstructor
public class InternalDeliveryController {

    private final DeliveryAssignmentService assignmentService;

    @PostMapping("/broadcast")
    public ApiResponse<Void> broadcast(@Valid @RequestBody BroadcastOrderRequest request) {
        assignmentService.broadcastNewOrder(request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<Void> cancel(@PathVariable String orderId) {
        assignmentService.cancelAssignmentsForOrder(orderId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{orderId}/manual-assign")
    public ApiResponse<Void> manualAssign(@PathVariable String orderId, @Valid @RequestBody ManualAssignRequest request) {
        assignmentService.manualAssign(orderId, request);
        return ApiResponse.ok(null);
    }
}
