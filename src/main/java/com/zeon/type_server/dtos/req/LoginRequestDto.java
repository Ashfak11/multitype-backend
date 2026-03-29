package com.zeon.type_server.dtos.req;

import lombok.Data;

@Data
public class LoginRequestDto {
    private String username;
    private String password;
}

