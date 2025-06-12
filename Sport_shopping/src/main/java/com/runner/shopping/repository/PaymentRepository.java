package com.runner.shopping.repository;

import com.runner.shopping.entity.Payments;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payments, Long> {
    Optional<Payments> findByOrderId(Long orderId);
    List<Payments> findAllByOrderId(Long orderId);
    Payments findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);
}
