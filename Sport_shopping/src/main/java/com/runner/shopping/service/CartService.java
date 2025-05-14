package com.runner.shopping.service;

import com.runner.shopping.model.dto.CartDTO;

import java.util.List;

public interface CartService {
    CartDTO addToCart(CartDTO cartDTO);
    CartDTO updateCart(Long cartId, Integer quantity);
    void deleteFromCart(Long cartId);
    List<CartDTO> getCartByUserId(Long userId);

}
