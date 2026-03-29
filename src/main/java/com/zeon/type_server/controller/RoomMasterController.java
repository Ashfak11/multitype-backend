package com.zeon.type_server.controller;

import com.zeon.type_server.dtos.ApiResponseDTO;
import com.zeon.type_server.dtos.RoomDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/rooms")
public interface RoomMasterController {

    /**
     * POST /api/rooms/create
     * Creates a new room. Authenticated player becomes the CREATOR.
     */
    @PostMapping("/create")
    ResponseEntity<ApiResponseDTO<?>> createRoom(Authentication authentication);

    /**
     * POST /api/rooms/join
     * Joins an existing WAITING room by room code.
     */
    @PostMapping("/join")
    ResponseEntity<ApiResponseDTO<?>> joinRoom(@RequestBody RoomDTO.JoinRoomRequest request,
                                               Authentication authentication);

    /**
     * GET /api/rooms/{roomCode}/state
     * Fetches current room state from cache.
     * Clients call this after connecting to WS to get the full picture.
     */
    @GetMapping("/{roomCode}/state")
    ResponseEntity<ApiResponseDTO<?>> getRoomState(@PathVariable String roomCode);

    @PostMapping("/{roomCode}/leave")
    ResponseEntity<ApiResponseDTO<?>> leaveRoom(@PathVariable String roomCode,
                                                Authentication authentication);
}

