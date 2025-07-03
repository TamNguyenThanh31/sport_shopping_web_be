// src/main/java/com/runner/shopping/service/impl/ReportServiceImpl.java
package com.runner.shopping.service.impl;

import com.runner.shopping.entity.ProductVariant;
import com.runner.shopping.model.dto.*;
import com.runner.shopping.repository.OrderRepository;
import com.runner.shopping.repository.ProductVariantRepository;
import com.runner.shopping.repository.UserRepository;
import com.runner.shopping.service.OrderService;
import com.runner.shopping.service.ReportService;
import com.runner.shopping.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final UserRepository           userRepository;
    private final OrderService             orderService;

    // Các status hợp lệ để tính doanh thu/lợi nhuận
    private static final List<OrderStatus> VALID_STATUSES =
            List.of(OrderStatus.CONFIRMED, OrderStatus.DELIVERED);

    //───────────────────────────────────────────────────────────────────────────────
    // 1. Số đơn và báo cáo “Hôm nay”
    //───────────────────────────────────────────────────────────────────────────────

    @Override
    public Long countOrdersToday() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        return orderRepository.countOrdersSince(startOfToday);
    }

    @Override
    public BigDecimal sumRevenueToday() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        return orderRepository.sumRevenueSince(VALID_STATUSES, startOfToday);
    }

    @Override
    public BigDecimal sumProfitToday() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        return orderRepository.sumProfitSince(VALID_STATUSES, startOfToday);
    }

    //───────────────────────────────────────────────────────────────────────────────
    // 2. Báo cáo doanh thu/lợi nhuận theo khoảng thời gian tùy chọn
    //───────────────────────────────────────────────────────────────────────────────

    @Override
    public BigDecimal sumRevenueBetween(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("startDate không được null");
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        return orderRepository.sumRevenueBetween(VALID_STATUSES, startDate, endDate);
    }

    @Override
    public BigDecimal sumProfitBetween(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("startDate không được null");
        }
        if (endDate == null) {
            endDate = LocalDateTime.now();
        }
        return orderRepository.sumProfitBetween(VALID_STATUSES, startDate, endDate);
    }

    //───────────────────────────────────────────────────────────────────────────────
    // 3. Tồn kho theo tên sản phẩm → danh sách biến thể
    //───────────────────────────────────────────────────────────────────────────────

    @Override
    public Map<String, List<ProductVariantInfoDTO>> getCurrentStockByName() {
        List<Object[]> rows = productVariantRepository.fetchActiveVariantsWithProductName();

        Map<String, List<ProductVariantInfoDTO>> stockMap = new HashMap<>();
        for (Object[] row : rows) {
            String productName = (String) row[0];
            String variantName = (String) row[1]; // sku
            Long   stock       = ((Number) row[2]).longValue();

            ProductVariantInfoDTO dto = new ProductVariantInfoDTO(variantName, stock);

            stockMap.computeIfAbsent(productName, k -> new ArrayList<>()).add(dto);
        }

        return stockMap;
    }

    //───────────────────────────────────────────────────────────────────────────────
    // 4. Chi tiết doanh thu đơn hàng trong khoảng thời gian
    //───────────────────────────────────────────────────────────────────────────────

    @Override
    public Page<ReportOrderDTO> revenueDetail(Long staffId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        // 1. Lấy Page<OrderDTO> (có sẵn orderDetails enriched)
        Page<OrderDTO> orders = orderService.getAllOrders(
                staffId,
                null,   // không lọc status
                null,   // không lọc userId
                from,
                to,
                pageable
        );

        // 2. Map từng OrderDTO → ReportOrderDTO
        return orders.map(orderDto -> {
            // 2.1. Lấy customerName từ User.username
            String customerName = userRepository.findById(orderDto.getUserId())
                    .map(u -> u.getUsername())
                    .orElse("N/A");

            // 2.2. Chuyển OrderDetailDTO → ReportOrderItemDTO
            var items = orderDto.getOrderDetails().stream()
                    .map(d -> {
                        BigDecimal lineProfit = d.getPriceAtTime()
                                .subtract(d.getCostAtTime())
                                .multiply(BigDecimal.valueOf(d.getQuantity()));

                        String sku = productVariantRepository
                                .findByIdNotDeleted(d.getVariantId())
                                .map(ProductVariant::getSku)
                                .orElse("");

                        return new ReportOrderItemDTO(
                                d.getProductName(),
                                sku,
                                d.getQuantity(),
                                d.getCostAtTime(),
                                d.getPriceAtTime(),
                                lineProfit
                        );
                    })
                    .toList();

            // 2.3. Tạo ReportOrderDTO với summary + items
            return new ReportOrderDTO(
                    orderDto.getId(),
                    orderDto.getCreatedAt(),
                    customerName,
                    orderDto.getTotalPrice(),
                    orderDto.getTotalCost(),
                    orderDto.getTotalProfit(),
                    items
            );
        });
    }
}

