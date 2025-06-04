package com.runner.shopping.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductVariantDTO {
    private Long id;
    private Long productId;
    private String size;
    private String color;
    private int stock;
    private BigDecimal costPrice;  // giá nhập
    private BigDecimal price;      // giá bán
    private String sku;
}
