package com.zeon.type_server.dao.repo;

import com.zeon.type_server.dao.model.PlayerGameStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlayerGameStatsRepository extends JpaRepository<PlayerGameStats, Integer> {

    List<PlayerGameStats> findByResultId(Integer resultId);

    List<PlayerGameStats> findByPlayerIdOrderByCreatedAtDesc(UUID playerId);
}