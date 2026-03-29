package com.zeon.type_server.cache;


import com.zeon.type_server.dao.model.RoomMaster;
import com.zeon.type_server.dao.model.RoomPlayers;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hot in-memory object for an active room.
 *
 * Lives in RoomCacheService (ConcurrentHashMap) from room creation → FINISHED.
 * roomCode is ONLY here — it is never saved to the DB.
 *
 * Evicted when:
 *   - A player finishes all words (GAME_OVER)
 *   - A player disconnects (ROOM_CLOSED)
 */
@Data
@Builder
public class LiveRoom {

    private Integer roomId;       // FK to room_master
    private String roomCode;      // e.g. "A3FX9K" — in-memory only

    private RoomMaster.RoomStatus status;
    private String wordsPayload;  // set on countdown = 0

    /** playerId → LivePlayer */
    private ConcurrentHashMap<UUID, LivePlayer> players;

    private Instant createdAt;
    private Instant startedAt;

    // -------------------------------------------------------------------------

    @Data
    @Builder
    public static class LivePlayer {
        private UUID playerId;
        private String username;
        private RoomPlayers.ParticipantRole role;
        private RoomPlayers.PlayerStatus status;
        private int currentWordIndex;
        private int currentCharIndex;// updated on every PROGRESS message
        private Instant readyAt;

        private Integer finalWpm;
        private Double finalAccuracy;
        private Integer finalTotalChars;
        private Integer finalCorrectChars;
    }

    // -------------------------------------------------------------------------

    /** Room is full when 2 players have joined. */
    public boolean isFull() {
        return players != null && players.size() == 2;
    }

    /** Returns true when all present players have pressed READY. */
    public boolean isEveryoneReady() {
        if (players == null || players.isEmpty()) return false;
        return players.values().stream()
                .allMatch(p -> p.getStatus() == RoomPlayers.PlayerStatus.READY);
    }
}
