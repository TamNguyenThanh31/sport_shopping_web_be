package com.runner.shopping.entity;

import com.runner.shopping.enums.PaymentMethod;
import com.runner.shopping.enums.PaymentStatus;
import com.runner.shopping.enums.converter.PaymentMethodConverter;
import com.runner.shopping.enums.converter.PaymentStatusConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Payments")
@Getter
@Setter
public class Payments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Convert(converter = PaymentMethodConverter.class)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Convert(converter = PaymentStatusConverter.class)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "client_ip")
    private String clientIp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
