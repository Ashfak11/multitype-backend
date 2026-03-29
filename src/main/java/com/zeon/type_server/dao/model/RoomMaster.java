package com.zeon.type_server.dao.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(name = "room_master")
@Data
public class RoomMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Integer roomId; // numeric PK for DB convenience

    private String roomCode;

    /**
     * WAITING  → room created, waiting for second player
     * RUNNING  → countdown done, game in progress
     * FINISHED → someone finished or disconnected
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RoomStatus roomStatus;

//    @Column(name = "game_time_seconds")
    private Integer gameTimeSeconds;

//    @Column(name = "words_payload", columnDefinition = "TEXT")
//    private String wordsPayload;

    @OneToMany(mappedBy = "roomMaster", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RoomPlayers> roomPlayers;


    public enum RoomStatus {
        WAITING, RUNNING, FINISHED
    }

}
