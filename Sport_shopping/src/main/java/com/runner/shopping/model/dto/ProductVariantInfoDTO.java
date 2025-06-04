package com.runner.shopping.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//Hiển thị trong báo cáo của giao diện admin
@Data
@AllArgsConstructor
public class ProductVariantInfoDTO {
    private String sku;
    private Long   stock;
}
