package com.cabbooking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Pushes live ride events to subscribed WebSocket clients.
 * Channel key = ride id; key 0 is the "new ride requests" channel for online drivers.
 */
@Service
public class RideEventPublisher {

    private final Map<Long, Set<WebSocketSession>> channels = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public void subscribe(Long rideId, WebSocketSession session) {
        channels.computeIfAbsent(rideId, k -> new CopyOnWriteArraySet<>()).add(session);
    }

    public void unsubscribe(WebSocketSession session) {
        channels.values().forEach(s -> s.remove(session));
    }

    public void broadcast(Long rideId, String type, Object data) {
        Set<WebSocketSession> sessions = channels.get(rideId);
        if (sessions == null || sessions.isEmpty()) return;
        try {
            String json = mapper.writeValueAsString(Map.of(
                    "type", type, "rideId", rideId, "data", data == null ? Map.of() : data));
            TextMessage msg = new TextMessage(json);
            for (WebSocketSession s : sessions) {
                if (s.isOpen()) {
                    try { s.sendMessage(msg); } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
    }

    /** Notify all online drivers watching the request pool. */
    public void broadcastNewRequest(Object data) {
        broadcast(0L, "NEW_REQUEST", data);
    }
}
