package com.zeon.type_server.service.impl;


import com.zeon.type_server.cache.LiveRoom;
import com.zeon.type_server.cache.RoomCacheService;
import com.zeon.type_server.dao.model.PlayerMaster;
import com.zeon.type_server.dao.model.RoomMaster;
import com.zeon.type_server.dao.model.RoomPlayers;
import com.zeon.type_server.dao.repo.RoomMasterRepository;
import com.zeon.type_server.dao.repo.RoomPlayersRepository;
import com.zeon.type_server.dtos.ApiResponseDTO;
import com.zeon.type_server.dtos.RoomDTO;
import com.zeon.type_server.service.RoomMasterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomMasterServiceImpl implements RoomMasterService {

    private final RoomMasterRepository roomMasterRepository;
    private final RoomPlayersRepository roomPlayersRepository;
    private final RoomCacheService roomCacheService;
    private final SimpMessagingTemplate messagingTemplate;


    private static final String ALLOWED_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no O/0 I/1
    private static final int CODE_LENGTH = 6;

    // =========================================================================
    // Create Room
    // =========================================================================

    @Override
    @Transactional
    public ApiResponseDTO<RoomDTO.CreateRoomResponse> createRoom(Authentication authentication) {
        log.info("<<START>> createRoom <<START>>");
        try {
            PlayerMaster creator = (PlayerMaster) authentication.getPrincipal();

            RoomMaster roomMaster = new RoomMaster();
            roomMaster.setRoomStatus(RoomMaster.RoomStatus.WAITING);
            RoomMaster savedRoom = roomMasterRepository.save(roomMaster);

            RoomPlayers creatorSlot = new RoomPlayers();
            creatorSlot.setRoomMaster(savedRoom);
            creatorSlot.setPlayerMaster(creator);
            creatorSlot.setRole(RoomPlayers.ParticipantRole.CREATOR);
            creatorSlot.setPlayerStatus(RoomPlayers.PlayerStatus.IDLE);
            roomPlayersRepository.save(creatorSlot);

            String roomCode = generateRoomCode();

            LiveRoom.LivePlayer creatorLive = LiveRoom.LivePlayer.builder()
                    .playerId(creator.getPlayerId())
                    .username(creator.getUsername())
                    .role(RoomPlayers.ParticipantRole.CREATOR)
                    .status(RoomPlayers.PlayerStatus.IDLE)
                    .currentWordIndex(0)
                    .build();

            ConcurrentHashMap<UUID, LiveRoom.LivePlayer> players = new ConcurrentHashMap<>();
            players.put(creator.getPlayerId(), creatorLive);

            LiveRoom liveRoom = LiveRoom.builder()
                    .roomId(savedRoom.getRoomId())
                    .roomCode(roomCode)
                    .status(RoomMaster.RoomStatus.WAITING)
                    .players(players)
                    .createdAt(Instant.now())
                    .build();

            roomCacheService.put(liveRoom);

            log.info("Room created: roomId={} roomCode={} by player={}", savedRoom.getRoomId(), roomCode, creator.getUsername());

            RoomDTO.CreateRoomResponse response = RoomDTO.CreateRoomResponse.builder()
                    .roomId(savedRoom.getRoomId())
                    .roomCode(roomCode)
                    .roomStatus(RoomMaster.RoomStatus.WAITING.name())
                    .creator(toSnapshot(creatorLive))
                    .build();

            log.info("<<END>> createRoom <<END>>");
            return new ApiResponseDTO<>(response, null, "Room created successfully", false);

        } catch (ClassCastException e) {
            log.error("Authentication principal type mismatch: {}", e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ApiResponseDTO<>(null, HttpStatus.UNAUTHORIZED, e.getMessage(), true);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Business rule violation in createRoom: {}", e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ApiResponseDTO<>(null, HttpStatus.BAD_REQUEST, e.getMessage(), true);

        } catch (Exception e) {
            log.error("Unexpected error in createRoom: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ApiResponseDTO<>(null, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), true);
        }
    }

    // =========================================================================
    // Join Room
    // =========================================================================

    @Override
    @Transactional
    public ApiResponseDTO<RoomDTO.JoinRoomResponse> joinRoom(RoomDTO.JoinRoomRequest request, Authentication authentication) {
        log.info("<<START>> joinRoom <<START>>");
        try {
            PlayerMaster joiner = (PlayerMaster) authentication.getPrincipal();
            String roomCode = request.getRoomCode();

            LiveRoom liveRoom = roomCacheService.get(roomCode)
                    .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomCode));

            if (liveRoom.isFull()) {
                throw new IllegalStateException("Room is already full");
            }
            if (liveRoom.getStatus() != RoomMaster.RoomStatus.WAITING) {
                throw new IllegalStateException("Room is no longer accepting players");
            }
            if (liveRoom.getPlayers().containsKey(joiner.getPlayerId())) {
                throw new IllegalStateException("You are already in this room");
            }

            RoomMaster roomMaster = roomMasterRepository.findById(liveRoom.getRoomId())
                    .orElseThrow(() -> new IllegalStateException("Room DB record not found"));

            RoomPlayers joinerSlot = new RoomPlayers();
            joinerSlot.setRoomMaster(roomMaster);
            joinerSlot.setPlayerMaster(joiner);
            joinerSlot.setRole(RoomPlayers.ParticipantRole.JOINER);
            joinerSlot.setPlayerStatus(RoomPlayers.PlayerStatus.IDLE);
            roomPlayersRepository.save(joinerSlot);

            LiveRoom.LivePlayer joinerLive = LiveRoom.LivePlayer.builder()
                    .playerId(joiner.getPlayerId())
                    .username(joiner.getUsername())
                    .role(RoomPlayers.ParticipantRole.JOINER)
                    .status(RoomPlayers.PlayerStatus.IDLE)
                    .currentWordIndex(0)
                    .build();

            liveRoom.getPlayers().put(joiner.getPlayerId(), joinerLive);

            List<RoomDTO.PlayerSnapshot> allSnapshots = toSnapshots(liveRoom);

            RoomDTO.JoinRoomResponse response = RoomDTO.JoinRoomResponse.builder()
                    .roomId(liveRoom.getRoomId())
                    .roomCode(roomCode)
                    .roomStatus(liveRoom.getStatus().name())
                    .players(allSnapshots)
                    .build();

            messagingTemplate.convertAndSend("/topic/room/" + roomCode,
                    RoomDTO.RoomEvent.builder()
                            .type("PLAYER_JOINED")
                            .payload(RoomDTO.PlayerJoinedPayload.builder()
                                    .newPlayer(toSnapshot(joinerLive))
                                    .allPlayers(allSnapshots)
                                    .roomStatus(liveRoom.getStatus().name())
                                    .build())
                            .build()
            );

            log.info("Player {} joined room {}", joiner.getUsername(), roomCode);
            log.info("<<END>> joinRoom <<END>>");
            return new ApiResponseDTO<>(response, null, "Joined room successfully", false);

        } catch (ClassCastException e) {
            log.error("Authentication principal type mismatch: {}", e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ApiResponseDTO<>(null, HttpStatus.UNAUTHORIZED, e.getMessage(), true);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Business rule violation in joinRoom: {}", e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ApiResponseDTO<>(null, HttpStatus.BAD_REQUEST, e.getMessage(), true);

        } catch (Exception e) {
            log.error("Unexpected error in joinRoom: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ApiResponseDTO<>(null, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), true);
        }
    }

    // =========================================================================
    // Room State
    // =========================================================================

    @Override
    public ApiResponseDTO<RoomDTO.RoomStateResponse> getRoomState(String roomCode) {
        log.info("<<START>> getRoomState <<START>>");
        try {
            LiveRoom liveRoom = roomCacheService.get(roomCode)
                    .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomCode));

            RoomDTO.RoomStateResponse response = RoomDTO.RoomStateResponse.builder()
                    .roomId(liveRoom.getRoomId())
                    .roomCode(roomCode)
                    .roomStatus(liveRoom.getStatus().name())
                    .wordsPayload(liveRoom.getWordsPayload())
                    .players(toSnapshots(liveRoom))
                    .build();

            log.info("<<END>> getRoomState <<END>>");
            return new ApiResponseDTO<>(response, null, "Room state fetched", false);

        } catch (IllegalArgumentException e) {
            log.error("Room not found: {}", e.getMessage());
            return new ApiResponseDTO<>(null, HttpStatus.NOT_FOUND, e.getMessage(), true);

        } catch (Exception e) {
            log.error("Unexpected error in getRoomState: {}", e.getMessage(), e);
            return new ApiResponseDTO<>(null, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), true);
        }
    }

    @Override
    @Transactional
    public ApiResponseDTO<?> leaveRoom(String roomCode, Authentication authentication) {
        log.info("<<START>> leaveRoom <<START>>");
        try {
            PlayerMaster player = (PlayerMaster) authentication.getPrincipal();

            LiveRoom liveRoom = roomCacheService.get(roomCode)
                    .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomCode));

            if (!liveRoom.getPlayers().containsKey(player.getPlayerId())) {
                throw new IllegalStateException("You are not in this room");
            }

            LiveRoom.LivePlayer leavingPlayer = liveRoom.getPlayers().get(player.getPlayerId());

            // Case 1 — game was running, treat same as disconnect
            if (liveRoom.getStatus() == RoomMaster.RoomStatus.RUNNING) {
                liveRoom.setStatus(RoomMaster.RoomStatus.FINISHED);
                persistRoomStatus(liveRoom.getRoomId(), RoomMaster.RoomStatus.FINISHED);

                messagingTemplate.convertAndSend("/topic/room/" + roomCode,
                        RoomDTO.RoomEvent.builder()
                                .type("ROOM_CLOSED")
                                .payload(RoomDTO.RoomClosedPayload.builder()
                                        .reason(leavingPlayer.getUsername() + " left the game")
                                        .roomStatus(RoomMaster.RoomStatus.FINISHED.name())
                                        .build())
                                .build()
                );

                roomCacheService.evict(roomCode);

                // Case 2 — still in lobby (WAITING), other player stays
            } else if (liveRoom.getStatus() == RoomMaster.RoomStatus.WAITING) {
                liveRoom.getPlayers().remove(player.getPlayerId());

                // If room is now empty, just evict silently
                if (liveRoom.getPlayers().isEmpty()) {
                    persistRoomStatus(liveRoom.getRoomId(), RoomMaster.RoomStatus.FINISHED);
                    roomCacheService.evict(roomCode);

                    // Other player is still there, notify them
                } else {
                    messagingTemplate.convertAndSend("/topic/room/" + roomCode,
                            RoomDTO.RoomEvent.builder()
                                    .type("PLAYER_LEFT")
                                    .payload(RoomDTO.PlayerLeftPayload.builder()
                                            .playerId(player.getPlayerId())
                                            .username(leavingPlayer.getUsername())
                                            .roomStatus(RoomMaster.RoomStatus.WAITING.name())
                                            .build())
                                    .build()
                    );
                }
            }

            log.info("Player {} left room {}", player.getUsername(), roomCode);
            log.info("<<END>> leaveRoom <<END>>");
            return new ApiResponseDTO<>(null, null, "Left room successfully", false);

        } catch (ClassCastException e) {
            log.error("Authentication principal type mismatch: {}", e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ApiResponseDTO<>(null, HttpStatus.UNAUTHORIZED, e.getMessage(), true);

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Business rule violation in leaveRoom: {}", e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ApiResponseDTO<>(null, HttpStatus.BAD_REQUEST, e.getMessage(), true);

        } catch (Exception e) {
            log.error("Unexpected error in leaveRoom: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return new ApiResponseDTO<>(null, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), true);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void persistRoomStatus(Integer roomId, RoomMaster.RoomStatus status) {
        roomMasterRepository.findById(roomId).ifPresent(room -> {
            room.setRoomStatus(status);
            roomMasterRepository.save(room);
        });
    }

    private String generateRoomCode() {
        SecureRandom rng = new SecureRandom();
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(ALLOWED_CHARS.charAt(rng.nextInt(ALLOWED_CHARS.length())));
            }
            code = sb.toString();
        } while (roomCacheService.exists(code));
        return code;
    }

    private RoomDTO.PlayerSnapshot toSnapshot(LiveRoom.LivePlayer player) {
        return RoomDTO.PlayerSnapshot.builder()
                .playerId(player.getPlayerId())
                .username(player.getUsername())
                .role(player.getRole())
                .playerStatus(player.getStatus())
                .currentWordIndex(player.getCurrentWordIndex())
                .build();
    }

    private List<RoomDTO.PlayerSnapshot> toSnapshots(LiveRoom liveRoom) {
        return liveRoom.getPlayers().values().stream()
                .map(this::toSnapshot)
                .collect(Collectors.toList());
    }
}
