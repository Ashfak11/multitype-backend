package com.zeon.type_server.cache;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for active rooms.
 *
 * Key   → roomCode  (the 6-char code the players share)
 * Value → LiveRoom
 *
 * This is intentionally a thin wrapper so it is easy to
 * swap for Redis later without touching service logic.
 */
@Service
public class RoomCacheService {

    private final ConcurrentHashMap<String, LiveRoom> cache = new ConcurrentHashMap<>();

    public void put(LiveRoom room) {
        cache.put(room.getRoomCode(), room);
    }

    public Optional<LiveRoom> get(String roomCode) {
        return Optional.ofNullable(cache.get(roomCode));
    }

    public void evict(String roomCode) {
        cache.remove(roomCode);
    }

    public boolean exists(String roomCode) {
        return cache.containsKey(roomCode);
    }
}
