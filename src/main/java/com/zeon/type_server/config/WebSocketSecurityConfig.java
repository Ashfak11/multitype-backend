package com.zeon.type_server.config;

import com.zeon.type_server.dao.model.PlayerMaster;
import com.zeon.type_server.dao.repo.PlayerMasterRepository;
import com.zeon.type_server.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;
import java.util.UUID;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;
    private final PlayerMasterRepository playerMasterRepository;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {

                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                // Only validate on CONNECT — the principal carries over for the session lifetime
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                        log.warn("WebSocket CONNECT rejected — missing or invalid Authorization header");
                        throw new IllegalArgumentException("Missing Authorization header");
                    }

                    String token = authHeader.substring(7);

                    try {
                        Claims claims = jwtUtil.validateToken(token);
                        String playerId = claims.getSubject();

                        PlayerMaster player = playerMasterRepository
                                .findById(UUID.fromString(playerId))
                                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        player,
                                        null,
                                        List.of(new SimpleGrantedAuthority("ROLE_" + player.getRole()))
                                );

                        // This is what sets principal.getName() = playerId for all WS messages
//                        accessor.setUser(authentication);
                        // Instead of setting the full UsernamePasswordAuthenticationToken
                        accessor.setUser(() -> player.getPlayerId().toString());

                        log.info("WebSocket CONNECT authenticated: playerId={} username={}",
                                playerId, player.getUsername());

                    } catch (Exception e) {
                        log.warn("WebSocket CONNECT rejected — invalid token: {}", e.getMessage());
                        throw new IllegalArgumentException("Invalid or expired token");
                    }
                }

                return message;
            }
        });
    }
}
