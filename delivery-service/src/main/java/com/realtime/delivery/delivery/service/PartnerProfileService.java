package com.realtime.delivery.delivery.service;

import com.realtime.delivery.delivery.dto.AvailabilityRequest;
import com.realtime.delivery.delivery.exception.DeliveryException;
import com.realtime.delivery.delivery.model.DeliveryPartnerProfile;
import com.realtime.delivery.delivery.model.PartnerStatus;
import com.realtime.delivery.delivery.repository.DeliveryPartnerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PartnerProfileService {

    private final DeliveryPartnerProfileRepository repository;

    @Transactional
    public void setAvailability(String partnerId, AvailabilityRequest request) {
        // BUSY is system-controlled (set on accept, cleared on delivered/cancel) - a partner can only toggle AVAILABLE/OFFLINE.
        if (request.getStatus() == PartnerStatus.BUSY) {
            throw DeliveryException.badRequest("Status BUSY is set automatically when you accept an order");
        }

        DeliveryPartnerProfile profile = repository.findById(partnerId)
                .orElse(DeliveryPartnerProfile.builder().partnerId(partnerId).build());

        // A partner mid-delivery can't go OFFLINE/AVAILABLE-toggle out of it here - status flips back to AVAILABLE automatically on DELIVERED.
        if (profile.getStatus() == PartnerStatus.BUSY) {
            throw DeliveryException.conflict("You have an active delivery in progress - finish it before changing availability");
        }

        profile.setStatus(request.getStatus());
        if (request.getLat() != null) profile.setLastLat(request.getLat());
        if (request.getLng() != null) profile.setLastLng(request.getLng());
        repository.save(profile);
    }

    @Transactional(readOnly = true)
    public DeliveryPartnerProfile getOrDefault(String partnerId) {
        return repository.findById(partnerId)
                .orElse(DeliveryPartnerProfile.builder().partnerId(partnerId).build());
    }
}
