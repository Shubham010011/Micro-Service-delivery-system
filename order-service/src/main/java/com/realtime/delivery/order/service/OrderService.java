package com.realtime.delivery.order.service;

import com.realtime.delivery.order.client.DeliveryServiceClient;
import com.realtime.delivery.order.dto.CreateOrderRequest;
import com.realtime.delivery.order.dto.OrderResponse;
import com.realtime.delivery.order.dto.UpdateOrderStatusRequest;
import com.realtime.delivery.order.exception.OrderException;
import com.realtime.delivery.order.model.Order;
import com.realtime.delivery.order.model.OrderStatus;
import com.realtime.delivery.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final DeliveryServiceClient deliveryServiceClient;

    private static final Set<OrderStatus> CANCELLABLE_STATES = Set.of(
            OrderStatus.PLACED, OrderStatus.LOOKING_FOR_PARTNER, OrderStatus.ASSIGNED, OrderStatus.ACCEPTED
    );

    /** Orders that can still be (re)assigned to a partner by an admin. */
    private static final Set<OrderStatus> ASSIGNABLE_STATES = Set.of(
            OrderStatus.PLACED, OrderStatus.LOOKING_FOR_PARTNER, OrderStatus.ASSIGNED, OrderStatus.REJECTED
    );

    private static final Set<OrderStatus> TERMINAL_STATES = Set.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED);

    @Transactional
    public OrderResponse placeOrder(String customerId, CreateOrderRequest request) {
        Order order = Order.builder()
                .customerId(customerId)
                .pickupAddress(request.getPickupAddress())
                .dropAddress(request.getDropAddress())
                .itemDescription(request.getItemDescription())
                .amount(request.getAmount())
                .status(OrderStatus.LOOKING_FOR_PARTNER)
                .preferredPartnerRequested(request.getPreferredDeliveryPartnerId() != null && !request.getPreferredDeliveryPartnerId().isBlank())
                .build();

        order = orderRepository.save(order);

        // Only tell delivery-service once the order row is committed - otherwise a fast partner could
        // accept it and delivery-service would ask us for an order we haven't committed yet.
        final String orderId = order.getId();
        final String pickup = order.getPickupAddress();
        final String drop = order.getDropAddress();
        final String preferred = request.getPreferredDeliveryPartnerId();
        afterCommit(() -> deliveryServiceClient.broadcastNewOrder(orderId, pickup, drop, preferred));

        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse cancelOrder(String customerId, String orderId) {
        Order order = getOwnedOrder(customerId, orderId);
        if (!CANCELLABLE_STATES.contains(order.getStatus())) {
            throw OrderException.conflict("Order can no longer be cancelled (current status: " + order.getStatus() + ")");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);
        afterCommit(() -> deliveryServiceClient.cancelAssignment(orderId));
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getCustomerOrders(String customerId) {
        return orderRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getCustomerOrder(String customerId, String orderId) {
        return OrderResponse.from(getOwnedOrder(customerId, orderId));
    }

    /** Internal-only lookup (no ownership check) - used when delivery-service/notification-service need order details by id. */
    @Transactional(readOnly = true)
    public OrderResponse getCustomerOrderUnsafe(String orderId) {
        return OrderResponse.from(findOrder(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getPartnerOrders(String deliveryPartnerId) {
        return orderRepository.findAllByAssignedDeliveryPartnerIdOrderByCreatedAtDesc(deliveryPartnerId)
                .stream().map(OrderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream().map(OrderResponse::from).toList();
    }

    @Transactional
    public OrderResponse adminAssign(String orderId, String deliveryPartnerId) {
        Order order = findOrder(orderId);
        if (!ASSIGNABLE_STATES.contains(order.getStatus())) {
            throw OrderException.conflict("Order cannot be assigned in its current status: " + order.getStatus());
        }
        order.setAssignedDeliveryPartnerId(deliveryPartnerId);
        order.setStatus(OrderStatus.ASSIGNED);
        order = orderRepository.save(order);
        final String pickup = order.getPickupAddress();
        final String drop = order.getDropAddress();
        afterCommit(() -> deliveryServiceClient.manualAssign(orderId, deliveryPartnerId, pickup, drop));
        return OrderResponse.from(order);
    }

    /** Called by delivery-service (internal) whenever the delivery lifecycle advances. */
    @Transactional
    public OrderResponse updateStatusInternal(String orderId, UpdateOrderStatusRequest request) {
        Order order = findOrder(orderId);
        if (TERMINAL_STATES.contains(order.getStatus()) && order.getStatus() != request.getStatus()) {
            // e.g. customer cancelled while the rider was mid-update - don't resurrect a cancelled/delivered order.
            throw OrderException.conflict("Order is already " + order.getStatus() + " and can no longer change status");
        }
        order.setStatus(request.getStatus());
        if (request.getStatus() == OrderStatus.LOOKING_FOR_PARTNER) {
            // Partner rejected -> the order is open again, so drop the stale assignment.
            order.setAssignedDeliveryPartnerId(null);
        } else if (request.getDeliveryPartnerId() != null && !request.getDeliveryPartnerId().isBlank()) {
            order.setAssignedDeliveryPartnerId(request.getDeliveryPartnerId());
        }
        order = orderRepository.save(order);
        return OrderResponse.from(order);
    }

    private Order findOrder(String orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> OrderException.notFound("Order not found"));
    }

    private Order getOwnedOrder(String customerId, String orderId) {
        Order order = findOrder(orderId);
        if (!order.getCustomerId().equals(customerId)) {
            throw OrderException.forbidden("You do not have access to this order");
        }
        return order;
    }

    /** Runs the (remote, non-transactional) call only after the surrounding DB transaction has committed. */
    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
