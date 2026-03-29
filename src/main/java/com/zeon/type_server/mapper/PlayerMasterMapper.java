package com.zeon.type_server.mapper;

import com.zeon.type_server.dao.model.PlayerMaster;
import com.zeon.type_server.dtos.PlayerMeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PlayerMasterMapper {

    // Entity to DTO
    PlayerMeResponse toDto(PlayerMaster entity);

    // DTO to Entity (if you need it later)
    // PlayerMaster toEntity(PlayerMeResponse dto);
}
