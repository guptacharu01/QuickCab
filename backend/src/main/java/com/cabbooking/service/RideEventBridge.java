package com.cabbooking.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Forwards ride events to WebSocket subscribers only AFTER the surrounding
 * database transaction commits. Broadcasting inside the transaction lets a
 * client receive the push and refetch before the change is saved, so the
 * dashboards show the old state (and the Pay button never appears).
 */
@Component
public class RideEventBridge {

    private final RideEventPublisher events;

    public RideEventBridge(RideEventPublisher events) {
        this.events = events;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRideEvent(RideWsEvent event) {
        events.broadcast(event.rideId(), event.type(), event.data());
    }
}
