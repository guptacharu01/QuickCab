package com.cabbooking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Plain WebSocket endpoint for live ride updates.
 * Connect: ws://localhost:8080/ws/rides?rideId=123&token=JWT
 * The frontend uses the native WebSocket API; no extra libraries needed.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RideWebSocketHandler handler;

    public WebSocketConfig(RideWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/rides").setAllowedOrigins("*");
    }
}
