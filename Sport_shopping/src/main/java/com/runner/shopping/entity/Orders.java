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

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    @Convert(converter = OrderStatusConverter.class)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Convert(converter = PaymentStatusConverter.class)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "handled_by")
    private Long handledBy;

    @Column(name = "address_id", nullable = false)
    private Long addressId;

    @Column(name = "promotion_id")
    private Long promotionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
