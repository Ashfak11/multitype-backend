package com.zeon.type_server.dtos;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class PlayerMeResponse {
    UUID playerId;
    String username;
    String role;
    LocalDateTime createdAt;
}

