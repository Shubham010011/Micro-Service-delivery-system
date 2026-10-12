package com.realtime.delivery.order.controller;

import com.realtime.delivery.common.dto.ApiResponse;
import com.realtime.delivery.order.dto.OrderResponse;
import com.realtime.delivery.order.dto.UpdateOrderStatusRequest;
import com.realtime.delivery.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Not routed through the API Gateway - only delivery-service calls this,
 * service-to-service inside the cluster, whenever a driver accepts/rejects
 * or advances the delivery status (picked up / in transit / delivered).
 */
@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderService orderService;

    @PatchMapping("/{orderId}/status")
    public ApiResponse<OrderResponse> updateStatus(@PathVariable String orderId,
                                                    @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ApiResponse.ok(orderService.updateStatusInternal(orderId, request));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable String orderId) {
        return ApiResponse.ok(orderService.getCustomerOrderUnsafe(orderId));
    }
}
