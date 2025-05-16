package com.runner.shopping.mapper;

import com.runner.shopping.entity.Promotions;
import com.runner.shopping.model.dto.PromotionDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PromotionMapper {

    @Mapping(target = "createdAt", ignore = true)
    Promotions toEntity(PromotionDTO promotionDTO);

    PromotionDTO toDTO(Promotions promotions);

    List<PromotionDTO> toDTOList(List<Promotions> promotions);
}
