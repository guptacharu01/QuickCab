package com.cabbooking.service;

import java.util.Map;

/**
 * A WebSocket push waiting to be sent. It is published as a Spring application
 * event and only forwarded to browsers AFTER the database transaction commits,
 * so a client that refetches on arrival never sees stale data.
 */
public record RideWsEvent(Long rideId, String type, Map<String, Object> data) {
}
