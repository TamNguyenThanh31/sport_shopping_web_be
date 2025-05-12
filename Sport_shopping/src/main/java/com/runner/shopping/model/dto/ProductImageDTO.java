package com.runner.shopping.model.dto;

import lombok.Data;

@Data
public class ProductImageDTO {
    private Long id;
    private Long productId;
    private String fileName;
    private boolean primary;
    private String imageUrl;
}
