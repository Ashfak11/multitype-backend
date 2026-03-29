package com.zeon.type_server.controller;

import com.zeon.type_server.dtos.RoomDTO;
import com.zeon.type_server.service.RoomGameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles all inbound STOMP messages and WebSocket lifecycle events.
 *
 * Clients subscribe to : /topic/room/{roomCode}
 * Clients send to      :
 *   /app/room/{roomCode}/ready     → player pressed READY
 *   /app/room/{roomCode}/progress  → player completed a word (ghost cursor)
 *   /app/room/{roomCode}/finish    → player finished all words
 *
 * Note: WS connection opens AFTER both players have joined via REST.
 * On connect the client subscribes to /topic/room/{roomCode} and
 * calls GET /api/rooms/{roomCode}/state to hydrate the lobby.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class RoomWebSocketController {

    private final RoomGameService roomGameService;

    /**
     * Maps WebSocket sessionId → ["roomCode", "playerId"]
     * Needed to resolve who disconnected on a raw session drop event.
     */
    private final ConcurrentHashMap<String, String[]> sessionRegistry = new ConcurrentHashMap<>();

    // =========================================================================
    // READY
    // =========================================================================

    @MessageMapping("/room/{roomCode}/ready")
    public void handleReady(
            @DestinationVariable String roomCode,
            Principal principal,
            StompHeaderAccessor accessor) {

        log.info("<<START>> handleReady roomCode={} <<START>>", roomCode);

        UUID playerId = resolvePlayerId(principal);
        String sessionId = accessor.getSessionId();

        // Register so disconnect handler can resolve roomCode + playerId
        sessionRegistry.put(sessionId, new String[]{roomCode, playerId.toString()});

        roomGameService.setPlayerReady(roomCode, playerId);

        log.info("<<END>> handleReady <<END>>");
    }

    // PROGRESS (ghost cursor)

    @MessageMapping("/room/{roomCode}/progress")
    public void handleProgress(
            @DestinationVariable String roomCode,
            @Payload RoomDTO.ProgressMessage message,
            Principal principal) {

        UUID playerId = resolvePlayerId(principal);
        roomGameService.updateProgress(roomCode, playerId, message.getCurrentWordIndex(), message.getCurrentCharIndex());
    }

    // =========================================================================
    // FINISH
    // =========================================================================

    @MessageMapping("/room/{roomCode}/finish")
    public void handleFinish(
            @DestinationVariable String roomCode,
            @Payload RoomDTO.FinishMessage message,
            Principal principal) {

        log.info("<<START>> handleFinish roomCode={} <<START>>", roomCode);

        UUID playerId = resolvePlayerId(principal);
        roomGameService.playerFinished(roomCode, playerId,message);

        log.info("<<END>> handleFinish <<END>>");
    }

    @MessageMapping("/room/{roomCode}/playAgain")
    public void handlePlayAgain(
            @DestinationVariable String roomCode,
            Principal principal) {
        UUID playerId = resolvePlayerId(principal);
        roomGameService.playerRequestPlayAgain(roomCode, playerId);
    }

    // =========================================================================
    // DISCONNECT
    // =========================================================================

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        String[] context = sessionRegistry.remove(sessionId);
        if (context == null) return; // session was never in a room

        String roomCode = context[0];
        UUID playerId   = UUID.fromString(context[1]);

        log.info("<<START>> handleSessionDisconnect sessionId={} roomCode={} playerId={} <<START>>",
                sessionId, roomCode, playerId);

        roomGameService.handleDisconnect(roomCode, playerId);

        log.info("<<END>> handleSessionDisconnect <<END>>");
    }

    // =========================================================================
    // Helper
    // =========================================================================

    /**
     * Resolves the authenticated player's UUID from the STOMP principal.
     *
     * Your JWT filter sets the Principal name to the player's UUID string.
     * If you use username as the JWT subject, swap this to a player lookup.
     */
    private UUID resolvePlayerId(Principal principal) {
        if (principal == null) {
            throw new IllegalStateException("No authenticated principal on WebSocket session");
        }
        return UUID.fromString(principal.getName());
    }
}
