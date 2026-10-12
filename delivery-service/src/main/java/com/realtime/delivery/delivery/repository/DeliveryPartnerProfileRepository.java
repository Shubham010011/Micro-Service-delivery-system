package com.realtime.delivery.delivery.repository;

import com.realtime.delivery.delivery.model.DeliveryPartnerProfile;
import com.realtime.delivery.delivery.model.PartnerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryPartnerProfileRepository extends JpaRepository<DeliveryPartnerProfile, String> {
    List<DeliveryPartnerProfile> findAllByStatus(PartnerStatus status);
}
