package com.realtime.delivery.delivery.repository;

import com.realtime.delivery.delivery.model.AssignmentStatus;
import com.realtime.delivery.delivery.model.DeliveryAssignment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, String> {
    List<DeliveryAssignment> findAllByOrderId(String orderId);
    Optional<DeliveryAssignment> findFirstByOrderIdAndStatusOrderByOfferedAtDesc(String orderId, AssignmentStatus status);
    List<DeliveryAssignment> findAllByDeliveryPartnerIdAndStatus(String deliveryPartnerId, AssignmentStatus status);
    Optional<DeliveryAssignment> findFirstByOrderIdOrderByOfferedAtDesc(String orderId);

    /**
     * SELECT ... FOR UPDATE on every assignment row of the order. Any concurrent accept / reject /
     * cancel / manual-assign for the SAME order blocks here until the first transaction commits, then
     * re-reads the fresh state. This is what makes "first accept wins" hold under concurrency.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from DeliveryAssignment a where a.orderId = :orderId order by a.offeredAt desc")
    List<DeliveryAssignment> findAllByOrderIdForUpdate(@Param("orderId") String orderId);
}
