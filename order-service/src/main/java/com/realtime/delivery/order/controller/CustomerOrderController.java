package com.realtime.delivery.order.controller;

import com.realtime.delivery.common.dto.ApiResponse;
import com.realtime.delivery.order.dto.CreateOrderRequest;
import com.realtime.delivery.order.dto.OrderResponse;
import com.realtime.delivery.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Only reachable by customers - the API Gateway rejects any request to
 * /api/customer/** whose JWT role is not CUSTOMER before it ever reaches
 * this service, and @PreAuthorize re-checks it here too (defense in depth).
 */
@RestController
@RequestMapping("/api/customer/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerOrderController {

    private final OrderService orderService;

    @PostMapping
    public ApiResponse<OrderResponse> placeOrder(@AuthenticationPrincipal String customerId,
                                                  @Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.ok("Order placed - looking for a delivery partner", orderService.placeOrder(customerId, request));
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> myOrders(@AuthenticationPrincipal String customerId) {
        return ApiResponse.ok(orderService.getCustomerOrders(customerId));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getOrder(@AuthenticationPrincipal String customerId,
                                                @PathVariable String orderId) {
        return ApiResponse.ok(orderService.getCustomerOrder(customerId, orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(@AuthenticationPrincipal String customerId,
                                                   @PathVariable String orderId) {
        return ApiResponse.ok("Order cancelled", orderService.cancelOrder(customerId, orderId));
    }
}
