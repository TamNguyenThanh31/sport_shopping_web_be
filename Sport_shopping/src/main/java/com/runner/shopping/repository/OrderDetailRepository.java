package com.runner.shopping.repository;

import com.runner.shopping.entity.OrderDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetails, Long> {

    List<OrderDetails> findByOrderId(Long orderId);
}
