package com.zeon.type_server.service.impl;

import com.zeon.type_server.dao.model.PlayerMaster;
import com.zeon.type_server.dao.repo.PlayerMasterRepository;
import com.zeon.type_server.dtos.req.LoginRequestDto;
import com.zeon.type_server.dtos.req.RegisterRequestDto;
import com.zeon.type_server.service.AuthService;
import com.zeon.type_server.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final PlayerMasterRepository playerMasterRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public void register(RegisterRequestDto dto) {

        if (dto.getUsername() == null || dto.getUsername().isBlank()) {
            throw new RuntimeException("Username is required");
        }

        validatePassword(dto.getPassword());

        if (playerMasterRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        PlayerMaster player = new PlayerMaster();
        player.setUsername(dto.getUsername());
        player.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        player.setRole("USER");
        player.setCreatedAt(LocalDateTime.now());

        playerMasterRepository.save(player);
    }

    @Override
    public String login(LoginRequestDto dto) {

        PlayerMaster player = playerMasterRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(dto.getPassword(), player.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        return jwtUtil.generateToken(player);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters");
        }
        if (!password.matches("^[a-zA-Z0-9]+$")) {
            throw new RuntimeException("Password can contain only letters and numbers");
        }
    }
}

