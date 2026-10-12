package com.realtime.delivery.order.repository;

import com.realtime.delivery.order.model.Order;
import com.realtime.delivery.order.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findAllByCustomerIdOrderByCreatedAtDesc(String customerId);
    List<Order> findAllByAssignedDeliveryPartnerIdOrderByCreatedAtDesc(String deliveryPartnerId);
    List<Order> findAllByStatusOrderByCreatedAtDesc(OrderStatus status);
    List<Order> findAllByOrderByCreatedAtDesc();
}
