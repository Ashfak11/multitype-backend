package com.zeon.type_server.service;

import com.zeon.type_server.dtos.ApiResponseDTO;
import com.zeon.type_server.dtos.RoomDTO;
import org.springframework.security.core.Authentication;


public interface RoomMasterService {

    ApiResponseDTO<RoomDTO.CreateRoomResponse> createRoom(Authentication authentication);

    ApiResponseDTO<RoomDTO.JoinRoomResponse> joinRoom(RoomDTO.JoinRoomRequest request, Authentication authentication);

    ApiResponseDTO<RoomDTO.RoomStateResponse> getRoomState(String roomCode);

    ApiResponseDTO<?> leaveRoom(String roomCode, Authentication authentication);

}
