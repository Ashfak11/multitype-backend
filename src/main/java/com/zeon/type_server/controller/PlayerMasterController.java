package com.zeon.type_server.controller;

import com.zeon.type_server.dtos.ApiResponseDTO;
import com.zeon.type_server.dtos.PlayerMeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequestMapping("/api/players")
public interface PlayerMasterController {

    @GetMapping("/me")
    ResponseEntity<ApiResponseDTO<?>> getCurrentPlayer(Authentication authentication);

    @GetMapping("/{playerId}")
    ResponseEntity<ApiResponseDTO<?>> getPlayerById(@PathVariable UUID playerId);

}