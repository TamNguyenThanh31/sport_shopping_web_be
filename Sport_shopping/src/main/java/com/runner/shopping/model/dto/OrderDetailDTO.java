package com.runner.shopping.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderDetailDTO {
    @Schema(description = "Order detail ID")
    private Long id;

    @Schema(description = "Product variant ID")
    @NotNull(message = "Variant ID is required")
    private Long variantId;

    @Schema(description = "Quantity ordered")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @Schema(description = "Cost price of the variant at the time of order")
    private BigDecimal costAtTime;

    @Schema(description = "Price of the variant at the time of order")
    private BigDecimal priceAtTime;

    @Schema(description = "Product name")
    private String productName;

    @Schema(description = "Variant size")
    private String size;

    @Schema(description = "Variant color")
    private String color;

    @Schema(description = "URL of the primary product image")
    private String imageUrl;
}