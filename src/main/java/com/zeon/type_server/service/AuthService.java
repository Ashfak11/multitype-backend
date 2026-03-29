package com.zeon.type_server.service;

import com.zeon.type_server.dtos.req.LoginRequestDto;
import com.zeon.type_server.dtos.req.RegisterRequestDto;

public interface AuthService {
    void register(RegisterRequestDto dto);
    String login(LoginRequestDto dto);
}
