package com.runner.shopping.model.dto;

import lombok.Data;

@Data
public class ProductImageDTO {
    private Long id;
    private Long productId;
    private String imageUrl;
    private boolean isPrimary;
}
