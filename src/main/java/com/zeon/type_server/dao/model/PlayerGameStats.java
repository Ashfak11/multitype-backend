package com.zeon.type_server.dao.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "player_game_stats")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerGameStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stat_id")
    private Integer statId;

    @Column(name = "result_id", nullable = false)
    private Integer resultId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "wpm", nullable = false)
    private Integer wpm;

    @Column(name = "accuracy", nullable = false, precision = 5, scale = 2)
    private BigDecimal accuracy;

    @Column(name = "total_chars_typed", nullable = false)
    private Integer totalCharsTyped;

    @Column(name = "correct_chars", nullable = false)
    private Integer correctChars;

    @Column(name = "finish_position", nullable = false)
    private Integer finishPosition;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
