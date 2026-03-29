package com.zeon.type_server.controller.impl;

import com.zeon.type_server.controller.RoomMasterController;
import com.zeon.type_server.dtos.ApiResponseDTO;
import com.zeon.type_server.dtos.RoomDTO;
import com.zeon.type_server.service.RoomMasterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RoomMasterControllerImpl implements RoomMasterController {

    private final RoomMasterService roomMasterService;

    @Override
    public ResponseEntity<ApiResponseDTO<?>> createRoom(Authentication authentication) {
        log.info("<<START>> createRoom <<START>>");

        ResponseEntity<ApiResponseDTO<?>> response =
                new ResponseEntity<>(roomMasterService.createRoom(authentication), HttpStatus.CREATED);

        log.info("<<END>> createRoom <<END>>");
        return response;
    }

    @Override
    public ResponseEntity<ApiResponseDTO<?>> joinRoom(RoomDTO.JoinRoomRequest request, Authentication authentication) {
        log.info("<<START>> joinRoom <<START>>");

        ResponseEntity<ApiResponseDTO<?>> response =
                new ResponseEntity<>(roomMasterService.joinRoom(request, authentication), HttpStatus.OK);

        log.info("<<END>> joinRoom <<END>>");
        return response;
    }

    @Override
    public ResponseEntity<ApiResponseDTO<?>> getRoomState(String roomCode) {
        log.info("<<START>> getRoomState <<START>>");

        ResponseEntity<ApiResponseDTO<?>> response =
                new ResponseEntity<>(roomMasterService.getRoomState(roomCode), HttpStatus.OK);

        log.info("<<END>> getRoomState <<END>>");
        return response;
    }

    @Override
    public ResponseEntity<ApiResponseDTO<?>> leaveRoom(String roomCode, Authentication authentication) {
        log.info("<<START>> leaveRoom <<START>>");

        ResponseEntity<ApiResponseDTO<?>> response =
                new ResponseEntity<>(roomMasterService.leaveRoom(roomCode, authentication), HttpStatus.OK);

        log.info("<<END>> leaveRoom <<END>>");
        return response;
    }
}
