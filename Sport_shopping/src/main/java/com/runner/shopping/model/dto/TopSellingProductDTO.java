package com.runner.shopping.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TopSellingProductDTO {
    private Long productId;
    private String productName;
    private Long variantId;
    private String variantSku;
    private Integer totalQuantitySold;
}