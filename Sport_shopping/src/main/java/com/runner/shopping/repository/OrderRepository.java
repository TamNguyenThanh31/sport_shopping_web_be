package com.runner.shopping.repository;

import com.runner.shopping.entity.Orders;
import com.runner.shopping.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Orders, Long> {

    List<Orders> findByUserId(Long userId);

    Optional<Orders> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT o FROM Orders o WHERE o.userId = :userId AND o.status = :status")
    List<Orders> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") OrderStatus status);

    boolean existsByAddressId(Long addressId);
}