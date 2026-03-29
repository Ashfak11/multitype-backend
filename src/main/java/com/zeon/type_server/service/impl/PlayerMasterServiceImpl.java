package com.zeon.type_server.service.impl;

import com.zeon.type_server.dao.model.PlayerMaster;
import com.zeon.type_server.dao.repo.PlayerMasterRepository;
import com.zeon.type_server.dtos.ApiResponseDTO;
import com.zeon.type_server.dtos.PlayerMeResponse;
import com.zeon.type_server.mapper.PlayerMasterMapper;
import com.zeon.type_server.service.PlayerMasterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerMasterServiceImpl implements PlayerMasterService {

    private final PlayerMasterRepository playerMasterRepository;
    private final PlayerMasterMapper playerMasterMapper;

    @Override
    public ApiResponseDTO<?> getCurrentPlayer(Authentication authentication) {
        try {
            log.info("Getting current player");

            PlayerMaster player = (PlayerMaster) authentication.getPrincipal();
            PlayerMeResponse dto = playerMasterMapper.toDto(player);

            return new ApiResponseDTO<>(dto, null, "Player details retrieved successfully", false);

        } catch (ClassCastException e) {
            log.error("Authentication principal type mismatch: {}", e.getMessage());
            return new ApiResponseDTO<>(null, HttpStatus.UNAUTHORIZED, e.getMessage(), true);

        } catch (Exception e) {
            log.error("Error in getCurrentPlayer: {}", e.getMessage(), e);
            return new ApiResponseDTO<>(null, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), true);
        }
    }

    @Override
    public ApiResponseDTO<?> getPlayerById(UUID playerId) {
        return null;
    }

    @Override
    public ApiResponseDTO<?> getPlayerByUsername(String username) {
        return null;
    }
}