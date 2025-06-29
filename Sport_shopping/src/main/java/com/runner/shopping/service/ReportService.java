package com.runner.shopping.service;

import com.runner.shopping.model.dto.ProductVariantInfoDTO;
import com.runner.shopping.model.dto.ReportOrderDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    /**
     * Trả về Page các đơn hàng trong khoảng [from, to], mỗi đơn có:
     *  - orderId, createdAt, customerName (username)
     *  - totalRevenue, totalCost, totalProfit
     *  - list items gồm productName, sku, quantity, costAtTime, priceAtTime, lineProfit
     */
    Page<ReportOrderDTO> revenueDetail(
            Long staffId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );

    default Page<ReportOrderDTO> revenueDetailToday(Long staffId, Pageable pageable) {
        return revenueDetail(
                staffId,
                LocalDateTime.now().toLocalDate().atStartOfDay(),
                LocalDateTime.now(),
                pageable
        );
    }
    default Page<ReportOrderDTO> revenueDetailThisWeek(Long staffId, Pageable pageable) {
        LocalDateTime monday = LocalDateTime.now()
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                .toLocalDate().atStartOfDay();
        return revenueDetail(staffId, monday, LocalDateTime.now(), pageable);
    }
    default Page<ReportOrderDTO> revenueDetailThisMonth(Long staffId, Pageable pageable) {
        LocalDateTime first = LocalDateTime.now()
                .withDayOfMonth(1)
                .toLocalDate().atStartOfDay();
        return revenueDetail(staffId, first, LocalDateTime.now(), pageable);
    }
}
