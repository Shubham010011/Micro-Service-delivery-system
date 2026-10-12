package com.realtime.delivery.delivery.repository;

import com.realtime.delivery.delivery.model.PendingDispatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PendingDispatchRepository extends JpaRepository<PendingDispatch, String> {

    /** Row lock so two delivery-service replicas never process the same order's retry at once. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PendingDispatch p where p.orderId = :orderId")
    Optional<PendingDispatch> findByIdForUpdate(@Param("orderId") String orderId);
}
