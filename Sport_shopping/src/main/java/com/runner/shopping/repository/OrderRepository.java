package com.runner.shopping.repository;

import com.runner.shopping.entity.Orders;
import com.runner.shopping.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Orders, Long> {

    List<Orders> findByUserId(Long userId);

    Optional<Orders> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT o FROM Orders o WHERE o.userId = :userId AND o.status = :status")
    List<Orders> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") OrderStatus status);

    boolean existsByAddressId(Long addressId);

    @Query("SELECT o FROM Orders o WHERE " +
            "(:status IS NULL OR o.status = :status) AND " +
            "(:userId IS NULL OR o.userId = :userId) AND " +
            "(:startDate IS NULL OR o.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR o.createdAt <= :endDate)")
    Page<Orders> findOrdersWithFilters(
            @Param("status") OrderStatus status,
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}