package com.runner.shopping.mapper;

import com.runner.shopping.entity.Product;
import com.runner.shopping.entity.ProductImage;
import com.runner.shopping.entity.ProductVariant;
import com.runner.shopping.model.dto.ProductDTO;
import com.runner.shopping.model.dto.ProductImageDTO;
import com.runner.shopping.model.dto.ProductVariantDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(source = "addedById", target = "addedBy")
    @Mapping(source = "active", target = "active")
    Product toEntity(ProductDTO productDTO);

    @Mapping(source = "addedBy", target = "addedById")
    @Mapping(source = "active", target = "active")
    ProductDTO toDTO(Product product);

    ProductVariant toEntity(ProductVariantDTO productVariantDTO);

    ProductVariantDTO toDTO(ProductVariant productVariant);

    ProductImage toEntity(ProductImageDTO productImageDTO);

    ProductImageDTO toDTO(ProductImage productImage);

    List<ProductImageDTO> toImageDTOList(List<ProductImage> images);

    List<ProductVariantDTO> toVariantDTOList(List<ProductVariant> variants);
}
