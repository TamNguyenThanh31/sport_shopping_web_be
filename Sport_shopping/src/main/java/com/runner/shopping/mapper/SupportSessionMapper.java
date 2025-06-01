package com.runner.shopping.mapper;

import com.runner.shopping.entity.SupportSession;
import com.runner.shopping.model.dto.SupportSessionDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface SupportSessionMapper {
    SupportSessionMapper INSTANCE = Mappers.getMapper(SupportSessionMapper.class);

    @Mapping(source="staffId", target="staffId")
    SupportSessionDTO toDto(SupportSession session);
    @Mapping(source="staffId", target="staffId")
    SupportSession toEntity(SupportSessionDTO dto);
}
