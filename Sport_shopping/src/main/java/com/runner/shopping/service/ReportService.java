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

    /**
     * Tổng doanh thu trong khoảng thời gian [startDate, endDate]
     * @param startDate thời điểm bắt đầu (không null)
     * @param endDate thời điểm kết thúc (nếu null thì lấy thời điểm hiện tại)
     * @return tổng doanh thu
     */
    BigDecimal sumRevenueBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Tổng lợi nhuận trong khoảng thời gian [startDate, endDate]
     * @param startDate thời điểm bắt đầu (không null)
     * @param endDate thời điểm kết thúc (nếu null thì lấy thời điểm hiện tại)
     * @return tổng lợi nhuận
     */
    BigDecimal sumProfitBetween(LocalDateTime startDate, LocalDateTime endDate);

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

    default Page<ReportOrderDTO> revenueDetailByDateRange(Long staffId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        if (startDate == null) {
            throw new IllegalArgumentException("startDate không được null");
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate phải trước hoặc bằng endDate");
        }
        return revenueDetail(staffId, startDate, endDate, pageable);
    }
}
