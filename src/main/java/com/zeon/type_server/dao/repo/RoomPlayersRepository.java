package com.zeon.type_server.dao.repo;

import com.zeon.type_server.dao.model.RoomPlayers;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoomPlayersRepository extends JpaRepository<RoomPlayers, UUID> {
}
