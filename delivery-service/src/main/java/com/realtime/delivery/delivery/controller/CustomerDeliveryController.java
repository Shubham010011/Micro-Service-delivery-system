package com.realtime.delivery.delivery.controller;

import com.realtime.delivery.common.dto.ApiResponse;
import com.realtime.delivery.delivery.dto.*;
import com.realtime.delivery.delivery.service.ChatService;
import com.realtime.delivery.delivery.service.DeliveryAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/deliveries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerDeliveryController {

    private final DeliveryAssignmentService assignmentService;
    private final ChatService chatService;

    @GetMapping("/{orderId}/status")
    public ApiResponse<DeliveryAssignmentResponse> status(@AuthenticationPrincipal String customerId,
                                                          @PathVariable String orderId) {
        return ApiResponse.ok(assignmentService.getForCustomer(customerId, orderId));
    }

    @PostMapping("/{orderId}/chat")
    public ApiResponse<ChatMessageResponse> sendMessage(@AuthenticationPrincipal String customerId,
                                                         @PathVariable String orderId,
                                                         @Valid @RequestBody ChatMessageRequest request) {
        return ApiResponse.ok(chatService.sendMessage(customerId, "CUSTOMER", orderId, request.getMessage()));
    }

    @GetMapping("/{orderId}/chat")
    public ApiResponse<List<ChatMessageResponse>> chatHistory(@AuthenticationPrincipal String customerId, @PathVariable String orderId) {
        return ApiResponse.ok(chatService.history(customerId, orderId));
    }

}
