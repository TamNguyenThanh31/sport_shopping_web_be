// src/main/java/com/runner/shopping/service/impl/ReportServiceImpl.java
package com.runner.shopping.service.impl;

import com.runner.shopping.model.dto.ProductVariantInfoDTO;
import com.runner.shopping.repository.OrderRepository;
import com.runner.shopping.repository.ProductVariantRepository;
import com.runner.shopping.service.ReportService;
import com.runner.shopping.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final OrderRepository          orderRepository;
    private final ProductVariantRepository productVariantRepository;

    // Các status hợp lệ để tính doanh thu/lợi nhuận
    private static final List<OrderStatus> VALID_STATUSES =
            List.of(OrderStatus.CONFIRMED, OrderStatus.DELIVERED);

    //───────────────────────────────────────────────────────────────────────────────
    // 1. Số đơn và báo cáo “Hôm nay”
    //───────────────────────────────────────────────────────────────────────────────

    /** Đếm tổng số đơn từ 00:00 hôm nay đến giờ */
    @Override
    public Long countOrdersToday() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        return orderRepository.countOrdersSince(startOfToday);
    }

    /** Tổng doanh thu “hôm nay” (status = CONFIRMED || DELIVERED) */
    @Override
    public BigDecimal sumRevenueToday() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        return orderRepository.sumRevenueSince(VALID_STATUSES, startOfToday);
    }

    /** Tổng lợi nhuận “hôm nay” (status = CONFIRMED || DELIVERED) */
    @Override
    public BigDecimal sumProfitToday() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        return orderRepository.sumProfitSince(VALID_STATUSES, startOfToday);
    }

    //───────────────────────────────────────────────────────────────────────────────
    // 2. Báo cáo theo Tuần / Tháng
    //───────────────────────────────────────────────────────────────────────────────

    /** Tổng doanh thu “tuần này” (từ thứ Hai 00:00 đến giờ) */
    @Override
    public BigDecimal sumRevenueThisWeek() {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeekDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDateTime startOfWeek = startOfWeekDate.atStartOfDay();
        return orderRepository.sumRevenueSince(VALID_STATUSES, startOfWeek);
    }

    /** Tổng lợi nhuận “tuần này” (từ thứ Hai 00:00 đến giờ) */
    @Override
    public BigDecimal sumProfitThisWeek() {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeekDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDateTime startOfWeek = startOfWeekDate.atStartOfDay();
        return orderRepository.sumProfitSince(VALID_STATUSES, startOfWeek);
    }

    /** Tổng doanh thu “tháng này” (từ ngày 1 00:00 đến giờ) */
    @Override
    public BigDecimal sumRevenueThisMonth() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonthDate = today.withDayOfMonth(1);
        LocalDateTime startOfMonth = startOfMonthDate.atStartOfDay();
        return orderRepository.sumRevenueSince(VALID_STATUSES, startOfMonth);
    }

    /** Tổng lợi nhuận “tháng này” (từ ngày 1 00:00 đến giờ) */
    @Override
    public BigDecimal sumProfitThisMonth() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonthDate = today.withDayOfMonth(1);
        LocalDateTime startOfMonth = startOfMonthDate.atStartOfDay();
        return orderRepository.sumProfitSince(VALID_STATUSES, startOfMonth);
    }

    //───────────────────────────────────────────────────────────────────────────────
    // 3. Tồn kho theo tên sản phẩm → danh sách biến thể
    //───────────────────────────────────────────────────────────────────────────────

    /**
     * Lấy tồn kho hiện tại, trả về Map<productName, List<ProductVariantInfoDTO>>
     * productVariantRepository.fetchActiveVariantsWithProductName() trả về mỗi Object[]:
     *   [0] = productName (String)
     *   [1] = variantName (String) (hay sku)
     *   [2] = stock       (Long)
     */
    @Override
    public Map<String, List<ProductVariantInfoDTO>> getCurrentStockByName() {
        List<Object[]> rows = productVariantRepository.fetchActiveVariantsWithProductName();

        Map<String, List<ProductVariantInfoDTO>> stockMap = new HashMap<>();
        for (Object[] row : rows) {
            String productName = (String) row[0];
            String variantName = (String) row[1]; //sku
            Long   stock       = ((Number) row[2]).longValue();

            ProductVariantInfoDTO dto = new ProductVariantInfoDTO(variantName, stock);

            stockMap
                    .computeIfAbsent(productName, k -> new ArrayList<>())
                    .add(dto);
        }

        return stockMap;
    }
}
