package com.runner.shopping.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderDetailDTO {
    private Long id;

    @NotNull(message = "Variant ID is required")
    private Long variantId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private BigDecimal priceAtTime;
}
