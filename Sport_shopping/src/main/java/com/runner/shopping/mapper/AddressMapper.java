package com.runner.shopping.mapper;

import com.runner.shopping.entity.Addresses;
import com.runner.shopping.model.dto.AddressDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "userId", ignore = true)
    Addresses toEntity(AddressDTO addressDTO);

    AddressDTO toDTO(Addresses addresses);

    List<AddressDTO> toDTOList(List<Addresses> addresses);
}
