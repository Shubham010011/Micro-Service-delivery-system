package com.realtime.delivery.delivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically expires stale offers and re-offers orders that still have no delivery partner. */
@Component
@RequiredArgsConstructor
@Slf4j
public class DispatchScheduler {

    private final DeliveryAssignmentService assignmentService;

    @Scheduled(fixedDelayString = "${dispatch.sweep-interval-ms:5000}", initialDelayString = "${dispatch.sweep-initial-delay-ms:15000}")
    public void sweep() {
        for (String orderId : assignmentService.pendingOrderIds()) {
            try {
                assignmentService.processPendingDispatch(orderId);
            } catch (Exception e) {
                // One bad order must never stop the others; it is retried on the next sweep.
                log.warn("Dispatch retry failed for order {}: {}", orderId, e.getMessage());
            }
        }
    }
}
