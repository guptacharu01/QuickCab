package com.cabbooking.config;

import com.cabbooking.entity.Ride;
import com.cabbooking.repository.RideRepository;
import com.cabbooking.security.JwtUtil;
import com.cabbooking.service.RideEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Handles ws://host/ws/rides?rideId=123&token=JWT (rideId=0 = driver request pool).
 * The token is validated on connect; only the ride's rider, its driver, or an
 * admin may subscribe to a ride channel.
 */
@Component
public class RideWebSocketHandler extends TextWebSocketHandler {

    private final RideEventPublisher publisher;
    private final JwtUtil jwtUtil;
    private final RideRepository rides;

    public RideWebSocketHandler(RideEventPublisher publisher, JwtUtil jwtUtil, RideRepository rides) {
        this.publisher = publisher;
        this.jwtUtil = jwtUtil;
        this.rides = rides;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        var params = UriComponentsBuilder.fromUri(session.getUri()).build().getQueryParams();
        String token = first(params.get("token"));
        String rideParam = first(params.get("rideId"));
        try {
            long rideId = Long.parseLong(rideParam);
            var claims = jwtUtil.parse(token).getPayload();
            Long uid = claims.get("uid", Number.class).longValue();
            String role = claims.get("role", String.class);
            if (rideId != 0 && !"ADMIN".equals(role)) {
                Ride ride = rides.findById(rideId).orElse(null);
                boolean isRider = ride != null && ride.getRider().getId().equals(uid);
                boolean isDriver = ride != null && ride.getDriver() != null && ride.getDriver().getId().equals(uid);
                if (!isRider && !isDriver) {
                    session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Not your ride"));
                    return;
                }
            }
            if (rideId == 0 && !"DRIVER".equals(role) && !"ADMIN".equals(role)) {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Drivers only"));
                return;
            }
            publisher.subscribe(rideId, session);
            session.sendMessage(new TextMessage("{\"type\":\"CONNECTED\",\"rideId\":" + rideId + "}"));
        } catch (Exception e) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Bad token or ride id"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        publisher.unsubscribe(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Client -> server messages are not used; all updates are pushed by the backend.
    }

    private String first(List<String> l) {
        return (l == null || l.isEmpty()) ? null : l.get(0);
    }
}
