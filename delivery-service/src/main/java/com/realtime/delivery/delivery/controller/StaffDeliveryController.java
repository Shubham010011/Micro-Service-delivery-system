package com.realtime.delivery.delivery.controller;

import com.realtime.delivery.common.dto.ApiResponse;
import com.realtime.delivery.delivery.dto.*;
import com.realtime.delivery.delivery.model.DeliveryStatus;
import com.realtime.delivery.delivery.service.ChatService;
import com.realtime.delivery.delivery.service.DeliveryAssignmentService;
import com.realtime.delivery.delivery.service.PartnerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Mounted at /staff/** - the gateway only forwards these paths when the
 * caller's JWT role is DELIVERY_PARTNER. Customers never see this path.
 */
@RestController
@RequestMapping("/staff/deliveries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DELIVERY_PARTNER')")
public class StaffDeliveryController {

    private final DeliveryAssignmentService assignmentService;
    private final PartnerProfileService partnerProfileService;
    private final ChatService chatService;

    @PutMapping("/availability")
    public ApiResponse<Void> setAvailability(@AuthenticationPrincipal String partnerId,
                                              @Valid @RequestBody AvailabilityRequest request) {
        partnerProfileService.setAvailability(partnerId, request);
        return ApiResponse.ok("Availability updated", null);
    }

    @GetMapping("/offers")
    public ApiResponse<List<DeliveryAssignmentResponse>> pendingOffers(@AuthenticationPrincipal String partnerId) {
        return ApiResponse.ok(assignmentService.myPendingOffers(partnerId));
    }

    @PostMapping("/{orderId}/accept")
    public ApiResponse<DeliveryAssignmentResponse> accept(@AuthenticationPrincipal String partnerId, @PathVariable String orderId) {
        return ApiResponse.ok("Order accepted", assignmentService.accept(partnerId, orderId));
    }

    @PostMapping("/{orderId}/reject")
    public ApiResponse<Void> reject(@AuthenticationPrincipal String partnerId, @PathVariable String orderId) {
        assignmentService.reject(partnerId, orderId);
        return ApiResponse.ok("Order rejected", null);
    }

    @PatchMapping("/{orderId}/status")
    public ApiResponse<DeliveryAssignmentResponse> updateStatus(@AuthenticationPrincipal String partnerId,
                                                                 @PathVariable String orderId,
                                                                 @Valid @RequestBody UpdateDeliveryStatusRequest request) {
        return ApiResponse.ok("Status updated", assignmentService.updateStatus(partnerId, orderId, request.getStatus()));
    }

    // ---- Chat with the customer ----

    @PostMapping("/{orderId}/chat")
    public ApiResponse<ChatMessageResponse> sendMessage(@AuthenticationPrincipal String partnerId,
                                                         @PathVariable String orderId,
                                                         @Valid @RequestBody ChatMessageRequest request) {
        return ApiResponse.ok(chatService.sendMessage(partnerId, "DELIVERY_PARTNER", orderId, request.getMessage()));
    }

    @GetMapping("/{orderId}/chat")
    public ApiResponse<List<ChatMessageResponse>> chatHistory(@AuthenticationPrincipal String partnerId, @PathVariable String orderId) {
        return ApiResponse.ok(chatService.history(partnerId, orderId));
    }

}
