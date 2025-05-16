package com.runner.shopping.service;


import com.runner.shopping.model.dto.OrderDTO;

import java.util.List;

public interface OrderService {

    OrderDTO createOrder(OrderDTO orderDTO);

    OrderDTO getOrderById(Long id, Long userId);

    List<OrderDTO> getOrdersByUserId(Long userId);
}
