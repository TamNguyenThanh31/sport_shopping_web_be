package com.runner.shopping.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
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
}
