package com.runner.shopping.mapper;

import com.runner.shopping.entity.Product;
import com.runner.shopping.entity.ProductImage;
import com.runner.shopping.entity.ProductVariant;
import com.runner.shopping.model.dto.ProductDTO;
import com.runner.shopping.model.dto.ProductImageDTO;
import com.runner.shopping.model.dto.ProductVariantDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    // Product
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", constant = "0")
    Product toEntity(ProductDTO productDTO);

    ProductDTO toDTO(Product product);

    // ProductVariant
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", constant = "0")
    @Mapping(target = "productId", ignore = true) // productId sẽ được set riêng
    ProductVariant toEntity(ProductVariantDTO variantDTO);

    ProductVariantDTO toDTO(ProductVariant variant);

    // ProductImage
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "deleted", constant = "0")
    @Mapping(target = "productId", ignore = true) // productId sẽ được set riêng
    ProductImage toEntity(ProductImageDTO imageDTO);

    ProductImageDTO toDTO(ProductImage image);

    // Ánh xạ danh sách
    Iterable<ProductDTO> toDTOList(Iterable<Product> products);
    Iterable<ProductVariantDTO> toVariantDTOList(Iterable<ProductVariant> variants);
    Iterable<ProductImageDTO> toImageDTOList(Iterable<ProductImage> images);
}
