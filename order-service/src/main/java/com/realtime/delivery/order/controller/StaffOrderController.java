package com.realtime.delivery.order.controller;

import com.realtime.delivery.common.dto.ApiResponse;
import com.realtime.delivery.order.dto.OrderResponse;
import com.realtime.delivery.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Mounted at /staff/** - like /admin/**, the gateway only forwards these
 * paths to callers whose JWT role is DELIVERY_PARTNER, so customers never
 * see or reach this endpoint.
 */
@RestController
@RequestMapping("/staff/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DELIVERY_PARTNER')")
public class StaffOrderController {

    private final OrderService orderService;

    @GetMapping
    public ApiResponse<List<OrderResponse>> myAssignedOrders(@AuthenticationPrincipal String deliveryPartnerId) {
        return ApiResponse.ok(orderService.getPartnerOrders(deliveryPartnerId));
    }
}
