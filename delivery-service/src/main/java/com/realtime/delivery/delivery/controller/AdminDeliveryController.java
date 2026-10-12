package com.realtime.delivery.delivery.controller;

import com.realtime.delivery.common.dto.ApiResponse;
import com.realtime.delivery.delivery.client.AuthServiceClient;
import com.realtime.delivery.delivery.model.DeliveryPartnerProfile;
import com.realtime.delivery.delivery.service.PartnerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/delivery-partners")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDeliveryController {

    private final AuthServiceClient authServiceClient;
    private final PartnerProfileService partnerProfileService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> listPartners() {
        List<Map<String, Object>> partners = authServiceClient.listByRole("DELIVERY_PARTNER");
        List<Map<String, Object>> enriched = partners.stream().map(p -> {
            String id = (String) p.get("id");
            DeliveryPartnerProfile profile = partnerProfileService.getOrDefault(id);
            return Map.<String, Object>of(
                    "id", id,
                    "name", p.get("name"),
                    "email", p.get("email"),
                    "active", p.get("active"),
                    "status", profile.getStatus().name(),
                    "currentOrderId", profile.getCurrentOrderId() == null ? "" : profile.getCurrentOrderId()
            );
        }).toList();
        return ApiResponse.ok(enriched);
    }
}
