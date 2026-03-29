package com.zeon.type_server.service.impl;

import java.util.Comparator;
import com.zeon.type_server.cache.LiveRoom;
import com.zeon.type_server.cache.RoomCacheService;
import com.zeon.type_server.dao.model.GameResult;
import com.zeon.type_server.dao.model.PlayerGameStats;
import com.zeon.type_server.dao.model.RoomMaster;
import com.zeon.type_server.dao.model.RoomPlayers;
import com.zeon.type_server.dao.repo.GameResultRepository;
import com.zeon.type_server.dao.repo.PlayerGameStatsRepository;
import com.zeon.type_server.dao.repo.RoomMasterRepository;
import com.zeon.type_server.dtos.RoomDTO;
import com.zeon.type_server.service.RoomGameService;
import com.zeon.type_server.service.WordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomGameServiceImpl implements RoomGameService {

    private final RoomCacheService roomCacheService;
    private final RoomMasterRepository roomMasterRepository;
    private final GameResultRepository gameResultRepository;
    private final PlayerGameStatsRepository playerGameStatsRepository;
    private final WordService wordService;
    private final SimpMessagingTemplate messagingTemplate;
    private final TaskScheduler taskScheduler;

    /** Active countdown futures — cancelled on disconnect */
    private final ConcurrentHashMap<String, ScheduledFuture<?>> countdownTasks = new ConcurrentHashMap<>();

    private static final String ROOM_TOPIC = "/topic/room/";

    // =========================================================================
    // Set Player Ready
    // =========================================================================

    @Override
    public void setPlayerReady(String roomCode, UUID playerId) {
        log.info("<<START>> setPlayerReady roomCode={} playerId={} <<START>>", roomCode, playerId);

        LiveRoom liveRoom = getOrThrow(roomCode);
        LiveRoom.LivePlayer player = getPlayerOrThrow(liveRoom, playerId);

        player.setStatus(RoomPlayers.PlayerStatus.READY);
        player.setReadyAt(Instant.now());

        boolean allReady = liveRoom.isEveryoneReady();

        broadcast(roomCode, RoomDTO.RoomEvent.builder()
                .type("PLAYER_READY")
                .payload(RoomDTO.PlayerReadyPayload.builder()
                        .playerId(playerId)
                        .username(player.getUsername())
                        .playerStatus(RoomPlayers.PlayerStatus.READY.name())
                        .allReady(allReady)
                        .build())
                .build());

        log.info("Player {} is READY in room {}. All ready: {}", player.getUsername(), roomCode, allReady);

        if (allReady) {
            startCountdown(roomCode);
        }

        log.info("<<END>> setPlayerReady <<END>>");
    }

    // =========================================================================
    // Countdown
    // =========================================================================

    private void startCountdown(String roomCode) {
        log.info("Countdown starting for room {}", roomCode);

        String words = wordService.generateWordSet(250);
        AtomicInteger count = new AtomicInteger(3);

        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(() -> {
            int current = count.getAndDecrement();

            if (current < 0) return;

            if (current == 0) {
                ScheduledFuture<?> self = countdownTasks.remove(roomCode);
                if (self != null) self.cancel(false);

                LiveRoom liveRoom = roomCacheService.get(roomCode).orElse(null);
                if (liveRoom == null) return;

                liveRoom.setStatus(RoomMaster.RoomStatus.RUNNING);
                liveRoom.setWordsPayload(words);
                liveRoom.setStartedAt(Instant.now());
                liveRoom.getPlayers().values()
                        .forEach(p -> p.setStatus(RoomPlayers.PlayerStatus.IN_GAME));

                broadcast(roomCode, RoomDTO.RoomEvent.builder()
                        .type("COUNTDOWN")
                        .payload(RoomDTO.CountdownPayload.builder()
                                .count(0)
                                .wordsPayload(words)
                                .build())
                        .build());

                log.info("Room {} status → RUNNING", roomCode);

            } else {
                broadcast(roomCode, RoomDTO.RoomEvent.builder()
                        .type("COUNTDOWN")
                        .payload(RoomDTO.CountdownPayload.builder()
                                .count(current)
                                .build())
                        .build());

                log.info("Room {} countdown tick: {}", roomCode, current);
            }

        }, Duration.ofSeconds(1));

        countdownTasks.put(roomCode, future);
    }

    // =========================================================================
    // Progress Update
    // =========================================================================

    @Override
    public void updateProgress(String roomCode, UUID playerId, int currentWordIndex, int currentCharIndex) {
        LiveRoom liveRoom = getOrThrow(roomCode);

        if (liveRoom.getStatus() != RoomMaster.RoomStatus.RUNNING) return;

        LiveRoom.LivePlayer player = getPlayerOrThrow(liveRoom, playerId);
        player.setCurrentWordIndex(currentWordIndex);
        player.setCurrentCharIndex(currentCharIndex);

        int totalWords = liveRoom.getWordsPayload() == null
                ? 0
                : liveRoom.getWordsPayload().split("\\s+").length;

        broadcast(roomCode, RoomDTO.RoomEvent.builder()
                .type("PROGRESS_UPDATE")
                .payload(RoomDTO.ProgressPayload.builder()
                        .playerId(playerId)
                        .username(player.getUsername())
                        .currentWordIndex(currentWordIndex)
                        .currentCharIndex(currentCharIndex)
                        .totalWords(totalWords)
                        .build())
                .build());
    }

    // =========================================================================
    // Player Finished
    // =========================================================================

    @Override
    @Transactional
    public void playerFinished(String roomCode, UUID playerId, RoomDTO.FinishMessage stats) {
        log.info("<<START>> playerFinished roomCode={} playerId={} <<START>>", roomCode, playerId);

        // Validate stats
        if (stats.getWpm() > 300 || stats.getWpm() < 0) {
            log.warn("Suspicious WPM: {} from player {}", stats.getWpm(), playerId);
            stats.setWpm(Math.min(stats.getWpm(), 300));
        }
        if (stats.getAccuracy() > 100 || stats.getAccuracy() < 0) {
            log.warn("Invalid accuracy: {} from player {}", stats.getAccuracy(), playerId);
            throw new IllegalArgumentException("Invalid accuracy value");
        }

        LiveRoom liveRoom = getOrThrow(roomCode);
        LiveRoom.LivePlayer player = getPlayerOrThrow(liveRoom, playerId);

        // Store stats
        player.setFinalWpm(stats.getWpm());
        player.setFinalAccuracy(stats.getAccuracy());
        player.setFinalTotalChars(stats.getTotalCharsTyped());
        player.setFinalCorrectChars(stats.getCorrectChars());
        player.setStatus(RoomPlayers.PlayerStatus.FINISHED);

        log.info("Player {} submitted stats: WPM={}, Accuracy={}", player.getUsername(), stats.getWpm(), stats.getAccuracy());

        // Check if BOTH players have finished
        boolean allFinished = liveRoom.getPlayers().values().stream()
                .allMatch(p -> p.getStatus() == RoomPlayers.PlayerStatus.FINISHED);

        if (allFinished) {
            // Determine winner by HIGHEST WPM
            UUID winnerId = liveRoom.getPlayers().values().stream()
                    .max(Comparator.comparingInt(p -> p.getFinalWpm() != null ? p.getFinalWpm() : 0))
                    .map(LiveRoom.LivePlayer::getPlayerId)
                    .orElse(playerId);

            LiveRoom.LivePlayer winner = getPlayerOrThrow(liveRoom, winnerId);
            liveRoom.setStatus(RoomMaster.RoomStatus.FINISHED);

            persistRoomStatus(liveRoom.getRoomId(), RoomMaster.RoomStatus.FINISHED);

            broadcast(roomCode, RoomDTO.RoomEvent.builder()
                    .type("GAME_OVER")
                    .payload(RoomDTO.GameOverPayload.builder()
                            .winnerId(winnerId)
                            .winnerUsername(winner.getUsername())
                            .finalStandings(toSnapshots(liveRoom))
                            .roomStatus(RoomMaster.RoomStatus.FINISHED.name())
                            .build())
                    .build());

            saveGameResult(liveRoom, winnerId);
//            roomCacheService.evict(roomCode);

            log.info("Room {} FINISHED. Winner: {} (WPM: {})", roomCode, winner.getUsername(), winner.getFinalWpm());
        } else {
            log.info("Waiting for other player to finish in room {}", roomCode);
        }

        log.info("<<END>> playerFinished <<END>>");
    }

    @Override
    public void playerRequestPlayAgain(String roomCode, UUID playerId) {
        log.info("Player {} requested play again in room {}", playerId, roomCode);

        LiveRoom liveRoom = roomCacheService.get(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomCode));

        LiveRoom.LivePlayer player = getPlayerOrThrow(liveRoom, playerId);
        player.setStatus(RoomPlayers.PlayerStatus.READY);
        player.setReadyAt(Instant.now());

        // Reset player stats
        player.setCurrentWordIndex(0);
        player.setCurrentCharIndex(0);
        player.setFinalWpm(null);
        player.setFinalAccuracy(null);

        boolean bothReady = liveRoom.getPlayers().values().stream()
                .allMatch(p -> p.getStatus() == RoomPlayers.PlayerStatus.READY);

        broadcast(roomCode, RoomDTO.RoomEvent.builder()
                .type("PLAYER_READY")
                .payload(RoomDTO.PlayerReadyPayload.builder()
                        .playerId(playerId)
                        .username(player.getUsername())
                        .playerStatus(RoomPlayers.PlayerStatus.READY.name())
                        .allReady(bothReady)
                        .build())
                .build());

        if (bothReady) {
            liveRoom.setStatus(RoomMaster.RoomStatus.WAITING);
            startCountdown(roomCode);
        }
    }

    // =========================================================================
    // Handle Disconnect
    // =========================================================================

    @Override
    @Transactional
    public void handleDisconnect(String roomCode, UUID playerId) {
        log.info("<<START>> handleDisconnect roomCode={} playerId={} <<START>>", roomCode, playerId);

        roomCacheService.get(roomCode).ifPresent(liveRoom -> {

            String username = liveRoom.getPlayers()
                    .getOrDefault(playerId, LiveRoom.LivePlayer.builder().username("Unknown").build())
                    .getUsername();

            ScheduledFuture<?> countdown = countdownTasks.remove(roomCode);
            if (countdown != null) countdown.cancel(false);

            liveRoom.setStatus(RoomMaster.RoomStatus.FINISHED);
            persistRoomStatus(liveRoom.getRoomId(), RoomMaster.RoomStatus.FINISHED);

            broadcast(roomCode, RoomDTO.RoomEvent.builder()
                    .type("ROOM_CLOSED")
                    .payload(RoomDTO.RoomClosedPayload.builder()
                            .reason(username + " disconnected")
                            .roomStatus(RoomMaster.RoomStatus.FINISHED.name())
                            .build())
                    .build());

            roomCacheService.evict(roomCode);
            log.info("Room {} closed — {} disconnected", roomCode, username);
        });

        log.info("<<END>> handleDisconnect <<END>>");
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void broadcast(String roomCode, RoomDTO.RoomEvent event) {
        messagingTemplate.convertAndSend(ROOM_TOPIC + roomCode, event);
    }

    private LiveRoom getOrThrow(String roomCode) {
        return roomCacheService.get(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomCode));
    }

    private LiveRoom.LivePlayer getPlayerOrThrow(LiveRoom room, UUID playerId) {
        LiveRoom.LivePlayer player = room.getPlayers().get(playerId);
        if (player == null) throw new IllegalArgumentException("Player not in room: " + playerId);
        return player;
    }

    private List<RoomDTO.PlayerSnapshot> toSnapshots(LiveRoom liveRoom) {
        return liveRoom.getPlayers().values().stream()
                .map(p -> RoomDTO.PlayerSnapshot.builder()
                        .playerId(p.getPlayerId())
                        .username(p.getUsername())
                        .role(p.getRole())
                        .playerStatus(p.getStatus())
                        .currentWordIndex(p.getCurrentWordIndex())
                        .build())
                .collect(Collectors.toList());
    }

    private void persistRoomStatus(Integer roomId, RoomMaster.RoomStatus status) {
        roomMasterRepository.findById(roomId).ifPresent(room -> {
            room.setRoomStatus(status);
            roomMasterRepository.save(room);
        });
    }

    // =========================================================================
    // PHASE 2: Analytics Persistence
    // =========================================================================

    private void saveGameResult(LiveRoom liveRoom, UUID winnerId) {
        try {
            // Save game-level result
            GameResult gameResult = GameResult.builder()
                    .roomCode(liveRoom.getRoomCode())
                    .winnerId(winnerId)
                    .gameMode("WORD_RACE")
                    .startedAt(liveRoom.getStartedAt())
                    .finishedAt(Instant.now())
                    .build();

            gameResult = gameResultRepository.save(gameResult);

            // Save per-player stats
            for (LiveRoom.LivePlayer player : liveRoom.getPlayers().values()) {
                PlayerGameStats stats = PlayerGameStats.builder()
                        .resultId(gameResult.getResultId())
                        .playerId(player.getPlayerId())
                        .username(player.getUsername())
                        .wpm(player.getFinalWpm() != null ? player.getFinalWpm() : 0)
                        .accuracy(player.getFinalAccuracy() != null
                                ? BigDecimal.valueOf(player.getFinalAccuracy()).setScale(2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO)
                        .totalCharsTyped(player.getFinalTotalChars() != null ? player.getFinalTotalChars() : 0)
                        .correctChars(player.getFinalCorrectChars() != null ? player.getFinalCorrectChars() : 0)
                        .finishPosition(player.getPlayerId().equals(winnerId) ? 1 : 2)
                        .build();

                playerGameStatsRepository.save(stats);
            }

            log.info("Game analytics saved: resultId={} roomCode={}", gameResult.getResultId(), liveRoom.getRoomCode());

        } catch (Exception e) {
            log.error("Failed to save game analytics for room {}: {}", liveRoom.getRoomCode(), e.getMessage(), e);
            // Don't throw — analytics failure shouldn't break game completion
        }
    }
}