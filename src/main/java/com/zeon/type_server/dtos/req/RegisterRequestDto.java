package com.zeon.type_server.dtos.req;

import lombok.Data;

@Data
public class RegisterRequestDto {
    private String username;
    private String password;
}

