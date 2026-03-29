package com.zeon.type_server.dao.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "room_players")
@Data
public class RoomPlayers {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "participant_id")
    private UUID RoomPlayersId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private RoomMaster roomMaster;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerMaster playerMaster;

    /**
     * Role of this player in the room.
     * CREATOR → created the room (Player 1)
     * JOINER  → joined via room code (Player 2)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private ParticipantRole role;

    /**
     * IDLE    → just joined, hasn't pressed ready
     * READY   → pressed ready, awaiting countdown
     * IN_GAME → game is currently running
     * FINISHED → completed the text / game ended
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "player_status", nullable = false)
    private PlayerStatus playerStatus;


    public enum ParticipantRole {
        CREATOR, JOINER
    }

    public enum PlayerStatus {
        IDLE, READY, IN_GAME, FINISHED
    }

}
