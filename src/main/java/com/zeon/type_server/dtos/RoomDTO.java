package com.zeon.type_server.dtos;

import com.zeon.type_server.dao.model.RoomPlayers;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

public class RoomDTO {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PlayerLeftPayload {
        private UUID playerId;
        private String username;
        private String roomStatus;   // still "WAITING" — room is open again
    }

    // =========================================================================
    // REST — Create Room Response
    // =========================================================================

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreateRoomResponse {
        private Integer roomId;
        private String roomCode;          // generated in-memory, never saved to DB
        private String roomStatus;        // "WAITING"
        private PlayerSnapshot creator;
    }

    // =========================================================================
    // REST — Join Room Request / Response
    // =========================================================================

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class JoinRoomRequest {
        private String roomCode;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class JoinRoomResponse {
        private Integer roomId;
        private String roomCode;
        private String roomStatus;
        private List<PlayerSnapshot> players;
    }

    // =========================================================================
    // REST — Room State (reconnect / initial load)
    // =========================================================================

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoomStateResponse {
        private Integer roomId;
        private String roomCode;
        private String roomStatus;
        private String wordsPayload;           // null until both players are READY
        private List<PlayerSnapshot> players;
    }

    // =========================================================================
    // WebSocket — Generic broadcast envelope
    //
    // type values:
    //   PLAYER_JOINED   → someone joined the room
    //   PLAYER_READY    → a player pressed ready
    //   COUNTDOWN       → tick 3 / 2 / 1 / 0(GO)
    //   PROGRESS_UPDATE → ghost cursor moved
    //   GAME_OVER       → a player finished
    //   ROOM_CLOSED     → someone disconnected
    // =========================================================================

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoomEvent {
        private String type;
        private Object payload;
    }

    // =========================================================================
    // WebSocket — Payloads (attached to RoomEvent.payload)
    // =========================================================================

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PlayerJoinedPayload {
        private PlayerSnapshot newPlayer;
        private List<PlayerSnapshot> allPlayers;
        private String roomStatus;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PlayerReadyPayload {
        private UUID playerId;
        private String username;
        private String playerStatus;   // "READY"
        private boolean allReady;      // true → server will start countdown
    }

    /**
     * Broadcast every second: count = 3, 2, 1
     * On count = 0: wordsPayload is populated — this is when both clients
     * render the words and start their local game timer.
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CountdownPayload {
        private int count;
        private String wordsPayload;   // only present on count = 0
    }

    /**
     * Sent whenever a player completes a word.
     * The opponent uses currentWordIndex to move the ghost cursor.
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProgressPayload {
        private UUID playerId;
        private String username;
        private int currentWordIndex;
        private int currentCharIndex;
        private int totalWords;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GameOverPayload {
        private UUID winnerId;
        private String winnerUsername;
        private List<PlayerSnapshot> finalStandings; // includes wpm/accuracy for ALL players
        private String roomStatus;    // "FINISHED"
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FinishMessage {
        private int wpm;
        private double accuracy;
        private int totalCharsTyped;
        private int correctChars;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoomClosedPayload {
        private String reason;
        private String roomStatus;    // "FINISHED"
    }

    // =========================================================================
    // WebSocket — Inbound messages (client → server)
    // =========================================================================

    /** /app/room/{roomCode}/progress */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProgressMessage {
        private int currentWordIndex;
        private int currentCharIndex;
    }

    // =========================================================================
    // Shared
    // =========================================================================

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PlayerSnapshot {
        private UUID playerId;
        private String username;
        private RoomPlayers.ParticipantRole role;
        private RoomPlayers.PlayerStatus playerStatus;
        private int currentWordIndex;   // for ghost cursor
    }
}
