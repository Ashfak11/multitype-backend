package com.zeon.type_server.dao.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import jakarta.persistence.*;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "game_result")
@Data
@Builder

public class GameResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Integer resultId;

    @Column(name = "room_code", nullable = false, length = 10)
    private String roomCode;

    @Column(name = "winner_id")
    private UUID winnerId;

    @Column(name = "game_mode", length = 20)
    @Builder.Default
    private String gameMode = "WORD_RACE";

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at", nullable = false)
    private Instant finishedAt;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
