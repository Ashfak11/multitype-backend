package com.zeon.type_server.service;

import com.zeon.type_server.dao.model.PlayerMaster;
import com.zeon.type_server.dtos.ApiResponseDTO;
import org.springframework.security.core.Authentication;

import java.util.UUID;

public interface PlayerMasterService {
    ApiResponseDTO<?> getCurrentPlayer(Authentication player);
    ApiResponseDTO<?> getPlayerById(UUID playerId);
    ApiResponseDTO<?> getPlayerByUsername(String username);
}
