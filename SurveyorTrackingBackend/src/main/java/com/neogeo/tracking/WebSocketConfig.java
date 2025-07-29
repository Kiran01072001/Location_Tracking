package com.neogeo.tracking;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${spring.websocket.max-text-message-size:8192}")
    private int maxTextMessageSize;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${spring.websocket.max-binary-message-size:65536}")
    private int maxBinaryMessageSize;

    // Production configuration remains unchanged
    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        // Production endpoint with CORS support
        registry.addEndpoint("/ws/location")
                .setAllowedOriginPatterns("*")  // Allow all origins for development
                .withSockJS();

        // Additional non-production endpoints can be added conditionally
        if (!isProductionEnvironment()) {
            configureDevelopmentEndpoints(registry);
        }
    }

    // Helper methods for non-production configuration
    private boolean isProductionEnvironment() {
        // Check if the active profile is production
        return activeProfile != null && 
               (activeProfile.contains("prod") || 
                activeProfile.contains("production"));
    }

    private void configureDevelopmentEndpoints(StompEndpointRegistry registry) {
        // Heartbeat settings (in milliseconds)
        final long HEARTBEAT_INTERVAL = 10000;
        final long HEARTBEAT_TIMEOUT = 30000;

        // Configure with heartbeat support for development
        registry.addEndpoint("/ws/location")
                .setAllowedOriginPatterns("*")  // Allow connections from any origin
                .withSockJS()
                .setHeartbeatTime(HEARTBEAT_INTERVAL)
                .setDisconnectDelay(HEARTBEAT_TIMEOUT * 2);

        // Add native WebSocket endpoint for development with extensive browser support
        registry.addEndpoint("/ws/location-native")
                .setAllowedOriginPatterns("*");

        // Add debugging endpoint specifically for development testing
        registry.addEndpoint("/ws/location-debug")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setWebSocketEnabled(true)
                .setHeartbeatTime(5000)  // More frequent heartbeat for debugging
                .setDisconnectDelay(HEARTBEAT_TIMEOUT);

        System.out.println("WebSocket configured for development environment");
    }

    // Explicit WebSocket container configuration to apply message size limits
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(maxTextMessageSize);
        container.setMaxBinaryMessageBufferSize(maxBinaryMessageSize);
        return container;
    }
}
