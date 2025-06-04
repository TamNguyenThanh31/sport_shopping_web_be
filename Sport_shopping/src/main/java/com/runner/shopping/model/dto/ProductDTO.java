package com.runner.shopping.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long id;
    private String name;
    private String description;
    private Long categoryId;
    private String brand;
    private Long addedById;
    private boolean active;
    private List<ProductVariantDTO> variants;
    private List<ProductImageDTO> images;

    public ProductDTO(Long id, String name) {
        this.id   = id;
        this.name = name;
    }

}
