package com.runner.shopping.model.dto;

import lombok.Data;

@Data
public class ProductVariantDTO {
    private Long id;
    private Long productId;
    private String size;
    private String color;
    private int stock;
    private double price;
    private String sku;
}
