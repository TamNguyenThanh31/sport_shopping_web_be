package com.runner.shopping.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class ReportOrderDTO {
    private Long orderId;
    private LocalDateTime createdAt;
    private String customerName;
    private BigDecimal totalRevenue;   // orderDto.getTotalPrice()
    private BigDecimal totalCost;      // orderDto.getTotalCost()
    private BigDecimal totalProfit;    // orderDto.getTotalProfit()
    private List<ReportOrderItemDTO> items;
}
