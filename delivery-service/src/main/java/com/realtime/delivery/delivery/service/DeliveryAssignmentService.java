package com.realtime.delivery.delivery.service;

import com.realtime.delivery.delivery.client.NotificationServiceClient;
import com.realtime.delivery.delivery.client.OrderServiceClient;
import com.realtime.delivery.delivery.dto.BroadcastOrderRequest;
import com.realtime.delivery.delivery.dto.DeliveryAssignmentResponse;
import com.realtime.delivery.delivery.dto.ManualAssignRequest;
import com.realtime.delivery.delivery.exception.DeliveryException;
import com.realtime.delivery.delivery.model.AssignmentStatus;
import com.realtime.delivery.delivery.model.DeliveryAssignment;
import com.realtime.delivery.delivery.model.DeliveryPartnerProfile;
import com.realtime.delivery.delivery.model.DeliveryStatus;
import com.realtime.delivery.delivery.model.PendingDispatch;
import com.realtime.delivery.delivery.model.PartnerStatus;
import com.realtime.delivery.delivery.repository.DeliveryAssignmentRepository;
import com.realtime.delivery.delivery.repository.DeliveryPartnerProfileRepository;
import com.realtime.delivery.delivery.repository.PendingDispatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryAssignmentService {

    /** A partner who has not answered an offer after this long loses it (and it can go to someone else). */
    @Value("${dispatch.offer-ttl-seconds:90}")
    private long offerTtlSeconds;

    /** Minimum gap between two retry rounds for the same order. */
    @Value("${dispatch.retry-interval-seconds:10}")
    private long retryIntervalSeconds;

    /** If nobody accepts within this time the order is cancelled and the customer is told. */
    @Value("${dispatch.give-up-minutes:30}")
    private long giveUpMinutes;

    private static final Set<String> CLOSED_ORDER_STATES = Set.of("CANCELLED", "DELIVERED");

    private final DeliveryAssignmentRepository assignmentRepository;
    private final DeliveryPartnerProfileRepository partnerProfileRepository;
    private final OrderServiceClient orderServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final PendingDispatchRepository pendingDispatchRepository;

    /**
     * New order needs a rider. If a preferred partner was requested (Zomato-style
     * "same delivery partner again") and they're AVAILABLE, offer it to them first
     * and only fall back to broadcasting to everyone else if they reject.
     * Otherwise broadcast to all AVAILABLE partners at once - first to accept wins.
     * Partners who were already offered this order (accepted/rejected/expired) are never offered it twice.
     */
    @Transactional
    public void broadcastNewOrder(BroadcastOrderRequest request) {
        String orderId = request.getOrderId();
        registerPending(orderId, request.getPickupAddress(), request.getDropAddress());

        // Skip partners who rejected, already hold a live offer, or accepted. Partners whose earlier offer merely
        // EXPIRED (or was withdrawn) may be asked again on a later round.
        Set<AssignmentStatus> blocking = Set.of(AssignmentStatus.REJECTED, AssignmentStatus.OFFERED, AssignmentStatus.ACCEPTED);
        Set<String> alreadyOffered = assignmentRepository.findAllByOrderId(orderId).stream()
                .filter(a -> blocking.contains(a.getStatus()))
                .map(DeliveryAssignment::getDeliveryPartnerId)
                .collect(Collectors.toSet());

        String preferred = request.getPreferredDeliveryPartnerId();
        List<String> targets;

        if (preferred != null && !preferred.isBlank() && !alreadyOffered.contains(preferred)
                && partnerProfileRepository.findById(preferred)
                    .map(p -> p.getStatus() == PartnerStatus.AVAILABLE).orElse(false)) {
            targets = List.of(preferred);
        } else {
            targets = partnerProfileRepository.findAllByStatus(PartnerStatus.AVAILABLE)
                    .stream().map(DeliveryPartnerProfile::getPartnerId)
                    .filter(id -> !alreadyOffered.contains(id))
                    .toList();
        }

        if (targets.isEmpty()) {
            // No one available right now - the order stays LOOKING_FOR_PARTNER and the dispatch scheduler
            // keeps retrying until a partner comes online, someone accepts, or the give-up timeout is reached.
            return;
        }

        for (String partnerId : targets) {
            DeliveryAssignment assignment = DeliveryAssignment.builder()
                    .orderId(orderId)
                    .deliveryPartnerId(partnerId)
                    .pickupAddress(request.getPickupAddress())
                    .dropAddress(request.getDropAddress())
                    .status(AssignmentStatus.OFFERED)
                    .build();
            assignment = assignmentRepository.save(assignment);

            notificationServiceClient.notifyUser(partnerId, "NEW_ORDER_OFFER",
                    "New delivery request",
                    "Pickup: " + request.getPickupAddress() + " -> Drop: " + request.getDropAddress(),
                    Map.of("orderId", orderId, "assignmentId", assignment.getId()));
        }
    }

    @Transactional
    public void cancelAssignmentsForOrder(String orderId) {
        assignmentRepository.findAllByOrderIdForUpdate(orderId).forEach(a -> {
            if (a.getStatus() == AssignmentStatus.OFFERED || a.getStatus() == AssignmentStatus.ACCEPTED) {
                a.setStatus(AssignmentStatus.CANCELLED);
                assignmentRepository.save(a);
            }
        });
        pendingDispatchRepository.deleteById(orderId);
        partnerProfileRepository.findAllByStatus(PartnerStatus.BUSY).stream()
                .filter(p -> orderId.equals(p.getCurrentOrderId()))
                .forEach(p -> {
                    p.setStatus(PartnerStatus.AVAILABLE);
                    p.setCurrentOrderId(null);
                    partnerProfileRepository.save(p);
                });
    }

    /** Admin manually picked a partner - withdraw any other pending offers and make a direct offer to just that one rider. */
    @Transactional
    public void manualAssign(String orderId, ManualAssignRequest request) {
        List<DeliveryAssignment> existing = assignmentRepository.findAllByOrderIdForUpdate(orderId);
        if (existing.stream().anyMatch(a -> a.getStatus() == AssignmentStatus.ACCEPTED)) {
            throw DeliveryException.conflict("This order has already been accepted by a delivery partner");
        }
        registerPending(orderId, request.getPickupAddress(), request.getDropAddress());
        existing.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.OFFERED)
                .forEach(a -> { a.setStatus(AssignmentStatus.CANCELLED); assignmentRepository.save(a); });

        DeliveryAssignment assignment = DeliveryAssignment.builder()
                .orderId(orderId)
                .deliveryPartnerId(request.getDeliveryPartnerId())
                .pickupAddress(request.getPickupAddress())
                .dropAddress(request.getDropAddress())
                .status(AssignmentStatus.OFFERED)
                .build();
        assignment = assignmentRepository.save(assignment);
        notificationServiceClient.notifyUser(request.getDeliveryPartnerId(), "NEW_ORDER_OFFER",
                "New delivery request (assigned by admin)",
                "Pickup: " + request.getPickupAddress() + " -> Drop: " + request.getDropAddress(),
                Map.of("orderId", orderId, "assignmentId", assignment.getId()));
    }

    /**
     * First-accept-wins. All assignment rows of the order are locked (SELECT ... FOR UPDATE) before anything is
     * checked, so two partners accepting at the same instant are serialised: the second one blocks, then sees
     * the ACCEPTED row committed by the first and gets a 409. The @Version column is a further safety net.
     */
    @Transactional
    public DeliveryAssignmentResponse accept(String partnerId, String orderId) {
        List<DeliveryAssignment> all = assignmentRepository.findAllByOrderIdForUpdate(orderId);

        if (all.stream().anyMatch(a -> a.getStatus() == AssignmentStatus.ACCEPTED)) {
            throw DeliveryException.conflict("This order has already been accepted by another delivery partner");
        }
        DeliveryAssignment assignment = all.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.OFFERED && a.getDeliveryPartnerId().equals(partnerId))
                .findFirst()
                .orElseThrow(() -> DeliveryException.notFound("No pending offer for this order"));

        if (assignment.getOfferedAt() != null
                && assignment.getOfferedAt().isBefore(Instant.now().minusSeconds(offerTtlSeconds))) {
            throw DeliveryException.conflict("This offer has expired");
        }

        Map<String, Object> order = orderServiceClient.getOrder(orderId);
        if (CLOSED_ORDER_STATES.contains(String.valueOf(order.get("status")))) {
            throw DeliveryException.conflict("This order is no longer available");
        }

        DeliveryPartnerProfile profile = partnerProfileRepository.findById(partnerId)
                .orElse(DeliveryPartnerProfile.builder().partnerId(partnerId).build());
        if (profile.getStatus() == PartnerStatus.BUSY && !orderId.equals(profile.getCurrentOrderId())) {
            throw DeliveryException.conflict("Finish your current delivery before accepting another one");
        }

        assignment.setStatus(AssignmentStatus.ACCEPTED);
        assignment.setDeliveryStatus(DeliveryStatus.ACCEPTED);
        assignment.setRespondedAt(Instant.now());
        assignment = assignmentRepository.save(assignment);

        // Withdraw every other pending offer for the same order.
        final String winningId = assignment.getId();
        all.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.OFFERED && !a.getId().equals(winningId))
                .forEach(a -> { a.setStatus(AssignmentStatus.EXPIRED); assignmentRepository.save(a); });

        profile.setStatus(PartnerStatus.BUSY);
        profile.setCurrentOrderId(orderId);
        partnerProfileRepository.save(profile);
        pendingDispatchRepository.deleteById(orderId);

        // Throws (and rolls this whole transaction back) if order-service can't record it.
        orderServiceClient.updateOrderStatus(orderId, "ACCEPTED", partnerId);

        Object customerId = order.get("customerId");
        if (customerId != null) {
            notificationServiceClient.notifyUser(customerId.toString(), "ORDER_ACCEPTED",
                    "Delivery partner assigned",
                    "A delivery partner has accepted your order and is on the way.",
                    Map.of("orderId", orderId));
        }

        return DeliveryAssignmentResponse.from(assignment);
    }

    @Transactional
    public void reject(String partnerId, String orderId) {
        List<DeliveryAssignment> all = assignmentRepository.findAllByOrderIdForUpdate(orderId);

        DeliveryAssignment assignment = all.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.OFFERED && a.getDeliveryPartnerId().equals(partnerId))
                .findFirst()
                .orElseThrow(() -> DeliveryException.notFound("No pending offer for this order"));

        assignment.setStatus(AssignmentStatus.REJECTED);
        assignment.setRespondedAt(Instant.now());
        assignmentRepository.save(assignment);

        // If nobody else is still holding an offer, look for other available partners (never the ones already asked).
        boolean anyOtherPending = all.stream()
                .anyMatch(a -> a.getStatus() == AssignmentStatus.OFFERED || a.getStatus() == AssignmentStatus.ACCEPTED);
        if (!anyOtherPending) {
            // Order goes back to "looking for partner" (also clears an admin's manual assignment on the order).
            orderServiceClient.updateOrderStatus(orderId, "LOOKING_FOR_PARTNER", null);

            BroadcastOrderRequest rebroadcast = new BroadcastOrderRequest();
            rebroadcast.setOrderId(orderId);
            rebroadcast.setPickupAddress(assignment.getPickupAddress());
            rebroadcast.setDropAddress(assignment.getDropAddress());
            broadcastNewOrder(rebroadcast);
        }
    }

    @Transactional
    public DeliveryAssignmentResponse updateStatus(String partnerId, String orderId, DeliveryStatus newStatus) {
        DeliveryAssignment assignment = assignmentRepository.findAllByOrderIdForUpdate(orderId).stream()
                .filter(a -> a.getStatus() == AssignmentStatus.ACCEPTED && a.getDeliveryPartnerId().equals(partnerId))
                .findFirst()
                .orElseThrow(() -> DeliveryException.notFound("You do not have an active assignment for this order"));

        // Only the next step in ACCEPTED -> PICKED_UP -> IN_TRANSIT -> DELIVERED is allowed (no skipping, no going back).
        DeliveryStatus current = assignment.getDeliveryStatus() == null ? DeliveryStatus.ACCEPTED : assignment.getDeliveryStatus();
        if (newStatus.ordinal() != current.ordinal() + 1) {
            throw DeliveryException.conflict("Invalid status change: " + current + " -> " + newStatus);
        }

        assignment.setDeliveryStatus(newStatus);
        assignment = assignmentRepository.save(assignment);

        orderServiceClient.updateOrderStatus(orderId, newStatus.name(), partnerId);

        if (newStatus == DeliveryStatus.DELIVERED) {
            partnerProfileRepository.findById(partnerId).ifPresent(profile -> {
                profile.setStatus(PartnerStatus.AVAILABLE);
                profile.setCurrentOrderId(null);
                partnerProfileRepository.save(profile);
            });
        }

        Map<String, Object> order = orderServiceClient.getOrder(orderId);
        Object customerId = order.get("customerId");
        if (customerId != null) {
            notificationServiceClient.notifyUser(customerId.toString(), "STATUS_UPDATE", "Order status updated",
                    "Your order is now: " + newStatus, Map.of("orderId", orderId, "status", newStatus.name()));
        }

        return DeliveryAssignmentResponse.from(assignment);
    }

    @Transactional(readOnly = true)
    public List<DeliveryAssignmentResponse> myPendingOffers(String partnerId) {
        Instant cutoff = Instant.now().minusSeconds(offerTtlSeconds);
        return assignmentRepository.findAllByDeliveryPartnerIdAndStatus(partnerId, AssignmentStatus.OFFERED)
                .stream()
                .filter(a -> a.getOfferedAt() == null || a.getOfferedAt().isAfter(cutoff))
                .map(DeliveryAssignmentResponse::from).toList();
    }

    // ------------------------------------------------------------------ dispatch retry / expiry

    @Transactional(readOnly = true)
    public List<String> pendingOrderIds() {
        return pendingDispatchRepository.findAll().stream().map(PendingDispatch::getOrderId).toList();
    }

    /**
     * One retry round for an order that still has no partner: expire stale offers, put a stuck ASSIGNED order back
     * to LOOKING_FOR_PARTNER, offer it to every AVAILABLE partner not yet holding/rejecting it, or give up after
     * {@code dispatch.give-up-minutes}. Idempotent and safe to run on several replicas (row-locked).
     */
    @Transactional
    public void processPendingDispatch(String orderId) {
        // Lock order is always: assignment rows first, then the pending row - the same order accept()/cancel use,
        // so the two can never deadlock each other.
        List<DeliveryAssignment> all = assignmentRepository.findAllByOrderIdForUpdate(orderId);
        PendingDispatch pending = pendingDispatchRepository.findByIdForUpdate(orderId).orElse(null);
        if (pending == null) return; // accepted / cancelled meanwhile

        Instant now = Instant.now();
        if (pending.getLastAttemptAt() != null
                && pending.getLastAttemptAt().isAfter(now.minusSeconds(retryIntervalSeconds))) {
            return; // another replica (or the initial broadcast) just tried
        }

        if (all.stream().anyMatch(a -> a.getStatus() == AssignmentStatus.ACCEPTED)) {
            pendingDispatchRepository.delete(pending);
            return;
        }

        Map<String, Object> order;
        try {
            order = orderServiceClient.getOrder(orderId);
        } catch (DeliveryException e) {
            if (e.getStatus() == HttpStatus.NOT_FOUND) {
                pendingDispatchRepository.delete(pending); // order no longer exists
            }
            return; // order-service unavailable: try again next round
        }
        String orderStatus = String.valueOf(order.get("status"));
        if (CLOSED_ORDER_STATES.contains(orderStatus)) {
            pendingDispatchRepository.delete(pending);
            return;
        }

        // 1) expire offers nobody answered in time
        Instant cutoff = now.minusSeconds(offerTtlSeconds);
        for (DeliveryAssignment a : all) {
            if (a.getStatus() == AssignmentStatus.OFFERED && a.getOfferedAt() != null && a.getOfferedAt().isBefore(cutoff)) {
                a.setStatus(AssignmentStatus.EXPIRED);
                a.setRespondedAt(now);
                assignmentRepository.save(a);
            }
        }

        pending.setLastAttemptAt(now);
        pendingDispatchRepository.save(pending);

        // 2) too long without a partner -> cancel and tell the customer
        if (Duration.between(pending.getCreatedAt(), now).toMinutes() >= giveUpMinutes) {
            giveUp(pending, all, order);
            return;
        }

        // 3) someone still has a live offer -> wait for them
        if (all.stream().anyMatch(a -> a.getStatus() == AssignmentStatus.OFFERED)) {
            return;
        }

        // 4) nobody holds an offer. If an admin's manual assignment lapsed, the order is open again.
        if ("ASSIGNED".equals(orderStatus)) {
            orderServiceClient.updateOrderStatus(orderId, "LOOKING_FOR_PARTNER", null);
        }
        BroadcastOrderRequest retry = new BroadcastOrderRequest();
        retry.setOrderId(orderId);
        retry.setPickupAddress(pending.getPickupAddress());
        retry.setDropAddress(pending.getDropAddress());
        broadcastNewOrder(retry);
    }

    private void giveUp(PendingDispatch pending, List<DeliveryAssignment> all, Map<String, Object> order) {
        String orderId = pending.getOrderId();
        log.info("No delivery partner accepted order {} within {} minutes - cancelling", orderId, giveUpMinutes);
        // Throws (rolling this round back) if order-service refuses, e.g. the customer cancelled at the same moment.
        orderServiceClient.updateOrderStatus(orderId, "CANCELLED", null);
        all.stream().filter(a -> a.getStatus() == AssignmentStatus.OFFERED).forEach(a -> {
            a.setStatus(AssignmentStatus.CANCELLED);
            assignmentRepository.save(a);
        });
        pendingDispatchRepository.delete(pending);
        Object customerId = order.get("customerId");
        if (customerId != null) {
            notificationServiceClient.notifyUser(customerId.toString(), "ORDER_CANCELLED",
                    "No delivery partner available",
                    "Sorry, we couldn't find a delivery partner for your order, so it was cancelled. Please try again.",
                    Map.of("orderId", orderId));
        }
    }

    private void registerPending(String orderId, String pickup, String drop) {
        if (!pendingDispatchRepository.existsById(orderId)) {
            pendingDispatchRepository.save(PendingDispatch.builder()
                    .orderId(orderId).pickupAddress(pickup).dropAddress(drop).build());
        }
    }

    /** Customer-facing: only the customer who owns the order may see its delivery assignment. */
    @Transactional(readOnly = true)
    public DeliveryAssignmentResponse getForCustomer(String customerId, String orderId) {
        Map<String, Object> order = orderServiceClient.getOrder(orderId);
        if (!customerId.equals(order.get("customerId"))) {
            throw DeliveryException.forbidden("You do not have access to this order");
        }
        return assignmentRepository.findFirstByOrderIdAndStatusOrderByOfferedAtDesc(orderId, AssignmentStatus.ACCEPTED)
                .or(() -> assignmentRepository.findFirstByOrderIdOrderByOfferedAtDesc(orderId))
                .map(DeliveryAssignmentResponse::from)
                .orElseThrow(() -> DeliveryException.notFound("No delivery partner has been assigned to this order yet"));
    }
}
