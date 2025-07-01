package com.runner.shopping.entity;

import com.runner.shopping.enums.OrderStatus;
import com.runner.shopping.enums.PaymentStatus;
import com.runner.shopping.enums.converter.OrderStatusConverter;
import com.runner.shopping.enums.converter.PaymentStatusConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Orders")
@Getter
@Setter
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Tổng tiền sau khi đã trừ khuyến mãi (được tính và gán trong service)
    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    // Tổng giá vốn (được tính trong service)
    @Column(name = "total_cost", nullable = false)
    private BigDecimal totalCost = BigDecimal.ZERO;

    // Tổng lợi nhuận = totalPrice - totalCost
    @Column(name = "total_profit", nullable = false)
    private BigDecimal totalProfit = BigDecimal.ZERO;

    // Trạng thái đơn: PENDING, CONFIRMED, DELIVERED, CANCELLED, v.v.
    @Convert(converter = OrderStatusConverter.class)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    // Trạng thái thanh toán: PENDING, COMPLETED, FAILED, CANCELLED, REFUNDED
    @Convert(converter = PaymentStatusConverter.class)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    // Ai xử lý đơn (staff/admin)
    @Column(name = "handled_by")
    private Long handledBy;

    @Column(name = "address_id", nullable = false)
    private Long addressId;

    @Column(name = "promotion_id")
    private Long promotionId;

    // Thời điểm tạo
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Thời điểm cập nhật
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Thời điểm hủy đơn (nếu có)
    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
