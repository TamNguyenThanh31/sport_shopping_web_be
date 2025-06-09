package com.runner.shopping.model.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PromotionDTO {
    private Long id;

    @NotBlank(message = "Code is required")
    private String code;

    @NotNull(message = "Discount percentage is required")
    @DecimalMin(value = "0.0", message = "Discount percentage must be at least 0")
    @DecimalMax(value = "100.0", message = "Discount percentage must not exceed 100")
    @JsonDeserialize(using = NumberDeserializers.BigDecimalDeserializer.class)
    private BigDecimal discountPercentage;

    // Thêm trường mới
    private BigDecimal discountAmount;

    @DecimalMin(value = "0.0", message = "Minimum order value must be at least 0")
    @JsonDeserialize(using = NumberDeserializers.BigDecimalDeserializer.class)
    private BigDecimal minimumOrderValue;

    private Integer maxUsage;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Boolean isActive;
}
