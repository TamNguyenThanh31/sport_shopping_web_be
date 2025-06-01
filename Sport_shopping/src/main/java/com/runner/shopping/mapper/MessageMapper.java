package com.runner.shopping.mapper;

import com.runner.shopping.entity.Message;
import com.runner.shopping.model.dto.MessageDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface MessageMapper {
    MessageMapper INSTANCE = Mappers.getMapper(MessageMapper.class);

    MessageDTO toDto(Message message);
    Message toEntity(MessageDTO messageDto);
}
