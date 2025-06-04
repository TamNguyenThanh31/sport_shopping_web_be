package com.runner.shopping.entity;

import lombok.Data;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ProductVariants")
@Data
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    private String size;

    private String color;

    @Column(nullable = false)
    private int stock;

    @Column(name = "cost_price", nullable = false)
    private BigDecimal costPrice; // giá nhập (mới) // giá bán hiện tại

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private String sku;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(nullable = false)
    private int deleted = 0;

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
