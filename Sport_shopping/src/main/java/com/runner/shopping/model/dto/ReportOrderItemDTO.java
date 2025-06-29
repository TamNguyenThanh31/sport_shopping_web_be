package com.runner.shopping.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ReportOrderItemDTO {
    private String productName;
    private String sku;
    private Integer quantity;
    private BigDecimal costAtTime;
    private BigDecimal priceAtTime;
    private BigDecimal lineProfit;      // (priceAtTime – costAtTime) × quantity
}
