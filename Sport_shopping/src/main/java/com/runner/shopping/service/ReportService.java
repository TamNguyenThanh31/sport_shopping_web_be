package com.runner.shopping.service;

import com.runner.shopping.model.dto.ProductVariantInfoDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ReportService {
    Long countOrdersToday();
    BigDecimal sumRevenueToday();
    BigDecimal sumProfitToday();
    Map<String, List<ProductVariantInfoDTO>> getCurrentStockByName();
    BigDecimal sumRevenueThisWeek();
    BigDecimal sumProfitThisWeek();

    BigDecimal sumRevenueThisMonth();
    BigDecimal sumProfitThisMonth();
}
