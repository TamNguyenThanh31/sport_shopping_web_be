package com.runner.shopping.mapper;

import com.runner.shopping.entity.Cart;
import com.runner.shopping.model.dto.CartDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CartMapper {
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Cart toEntity(CartDTO cartDTO);

    CartDTO toDTO(Cart cart);
}