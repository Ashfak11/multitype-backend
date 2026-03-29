package com.zeon.type_server.dao.repo;

import com.zeon.type_server.dao.model.RoomMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoomMasterRepository extends JpaRepository<RoomMaster, Integer> {
}
