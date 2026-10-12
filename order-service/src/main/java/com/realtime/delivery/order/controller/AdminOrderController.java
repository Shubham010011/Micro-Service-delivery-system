package com.realtime.delivery.order.controller;

import com.realtime.delivery.common.dto.ApiResponse;
import com.realtime.delivery.order.dto.AssignPartnerRequest;
import com.realtime.delivery.order.dto.OrderResponse;
import com.realtime.delivery.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Mounted at /admin/** - the gateway only forwards these paths when the
 * caller's JWT role is ADMIN, and never exposes/documents this path to the
 * customer-facing frontend, so ordinary customers never see it exists.
 */
@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ApiResponse<List<OrderResponse>> allOrders() {
        return ApiResponse.ok(orderService.getAllOrders());
    }

    @PostMapping("/{orderId}/assign")
    public ApiResponse<OrderResponse> assign(@PathVariable String orderId, @Valid @RequestBody AssignPartnerRequest request) {
        return ApiResponse.ok("Delivery partner assigned", orderService.adminAssign(orderId, request.getDeliveryPartnerId()));
    }
}
