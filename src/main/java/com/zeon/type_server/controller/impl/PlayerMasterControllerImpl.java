package com.zeon.type_server.controller.impl;

import com.zeon.type_server.controller.PlayerMasterController;
import com.zeon.type_server.dtos.ApiResponseDTO;
import com.zeon.type_server.service.PlayerMasterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PlayerMasterControllerImpl implements PlayerMasterController {

    private final PlayerMasterService playerMasterService;

    @Override
    public ResponseEntity<ApiResponseDTO<?>> getCurrentPlayer(Authentication authentication) {
        log.info("<<START>> getCurrentPlayer <<START>>");
        log.info("Auth class: {}", authentication.getPrincipal().getClass());

        ResponseEntity<ApiResponseDTO<?>> response =
                new ResponseEntity<>(playerMasterService.getCurrentPlayer(authentication), HttpStatus.OK);

        log.info("<<END>> getCurrentPlayer <<END>>");
        return response;
    }

    @Override
    public ResponseEntity<ApiResponseDTO<?>> getPlayerById(UUID playerId) {
        log.info("<<START>> getPlayerById <<START>>");

        ResponseEntity<ApiResponseDTO<?>> response =
                new ResponseEntity<>(playerMasterService.getPlayerById(playerId), HttpStatus.OK);

        log.info("<<END>> getPlayerById <<END>>");
        return response;
    }
}