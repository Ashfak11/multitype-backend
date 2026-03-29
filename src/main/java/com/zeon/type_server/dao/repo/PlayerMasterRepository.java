package com.zeon.type_server.dao.repo;

import com.zeon.type_server.dao.model.PlayerMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlayerMasterRepository extends JpaRepository<PlayerMaster, UUID> {

    Optional<PlayerMaster> findByUsername(String username);

    boolean existsByUsername(String username);
}
