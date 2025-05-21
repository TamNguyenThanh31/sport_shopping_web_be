package com.runner.shopping.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;


import java.math.BigDecimal;

@Data
public class CartDTO {
    private Long id;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Variant ID is required")
    private Long variantId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @Schema(description = "Available stock for the variant")
    private Integer stock;

    private BigDecimal priceAtTime;
    private String productName;
    private String imageUrl;
    private String size;
    private String color;
    private BigDecimal totalPrice;
}
