package com.runner.shopping.repository;

import com.runner.shopping.entity.Payments;
import com.runner.shopping.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payments, Long> {
    Optional<Payments> findByOrderId(Long orderId);
    List<Payments> findAllByOrderId(Long orderId);
    Payments findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);

    @Modifying
    @Query("""
        UPDATE Payments p SET
              p.status        = :status,
              p.refundAmount  = :refundAmount,
              p.refundedAt    = :refundedAt
        WHERE p.id = :id
    """)
    void updateStatusAndRefund(@Param("id")           Long paymentId,
                               @Param("status") PaymentStatus status,
                               @Param("refundAmount") BigDecimal refundAmount,
                               @Param("refundedAt") LocalDateTime refundedAt);
}
