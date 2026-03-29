package com.zeon.type_server.service;

import com.zeon.type_server.dtos.RoomDTO;

import java.util.UUID;

public interface RoomGameService {
    /**
     * Marks a player as READY.
     * If both players are READY, triggers the countdown sequence.
     */
    void setPlayerReady(String roomCode, UUID playerId);

    /**
     * Updates a player's current word index in the live cache
     * and broadcasts it to the room so the opponent can move their ghost cursor.
     */
    void updateProgress(String roomCode, UUID playerId, int currentWordIndex, int currentCharIndex);

    /**
     * Called when a player completes all words.
     * Broadcasts GAME_OVER, persists FINISHED status to DB, evicts the room.
     */
    void playerFinished(String roomCode, UUID playerId, RoomDTO.FinishMessage stats);

    void playerRequestPlayAgain(String roomCode, UUID playerId);
    /**
     * Called on WebSocket session disconnect.
     * Closes the room immediately, broadcasts ROOM_CLOSED, evicts cache.
     */
    void handleDisconnect(String roomCode, UUID playerId);
}
