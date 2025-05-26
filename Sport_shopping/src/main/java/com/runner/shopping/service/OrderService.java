package com.runner.shopping.service;


import com.runner.shopping.enums.OrderStatus;
import com.runner.shopping.model.dto.OrderDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderService {

    OrderDTO createOrder(OrderDTO orderDTO);

    OrderDTO getOrderById(Long id, Long userId);

    List<OrderDTO> getOrdersByUserId(Long userId);

    OrderDTO updateOrderStatus(Long orderId, OrderStatus newStatus, Long staffId);

    void cancelOrder(Long orderId, Long userId);

    Page<OrderDTO> getAllOrders(Long staffId, OrderStatus status, Long userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    String initiateVNPayPayment(Long orderId, Long userId, String returnUrl);
}
