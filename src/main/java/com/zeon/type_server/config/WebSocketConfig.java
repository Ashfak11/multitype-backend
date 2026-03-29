package com.zeon.type_server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/typing-ws") //WebSocket entry point
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Server → Client
        registry.enableSimpleBroker("/topic", "/queue"); //Broadcast destinations

        // Client → Server
        registry.setApplicationDestinationPrefixes("/app"); //Messages meant for controllers
    }
}

//wasn't working before cause of this
//@Override
//public void configureMessageBroker(MessageBrokerRegistry config) {
//    config.enableSimpleBroker("/room");      // server -> client
//    config.setApplicationDestinationPrefixes("/app"); // client -> server
//}
//
//@Override
//public void registerStompEndpoints(StompEndpointRegistry registry) {
//    registry.addEndpoint("/ws")
//            .setAllowedOriginPatterns("*")
//            .withSockJS();
//}
//}

//
//    private final GameWebSocketHandler gameWebSocketHandler;
//
//    public WebSocketConfig(GameWebSocketHandler gameWebSocketHandler) {
//        this.gameWebSocketHandler = gameWebSocketHandler;
//    }
//
//    @Override
//    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
//        registry.addHandler(gameWebSocketHandler, "/ws")
//                .setAllowedOrigins("http://127.0.0.1:5500");
//    }
//}

