package com.runner.shopping.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StravaRedeemRequest {
    @NotNull
    private Double amount;      // số lượng user nhập
    @NotBlank
    private String unit;        // "m" hoặc "km"
}