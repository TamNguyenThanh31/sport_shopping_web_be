package com.runner.shopping.controller;

import com.runner.shopping.model.dto.CartDTO;
import com.runner.shopping.service.CartService;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping
    public ResponseEntity<CartDTO> addToCart(@Valid @RequestBody CartDTO cartDTO) {
        return ResponseEntity.ok(cartService.addToCart(cartDTO));
    }

    @PutMapping("/{cartId}")
    public ResponseEntity<CartDTO> updateCart(@PathVariable Long cartId, @RequestParam @Min(1) Integer quantity) {
        CartDTO updatedCart = cartService.updateCart(cartId, quantity);
        return updatedCart != null ? ResponseEntity.ok(updatedCart) : ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{cartId}")
    public ResponseEntity<Void> deleteFromCart(@PathVariable Long cartId) {
        cartService.deleteFromCart(cartId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<CartDTO>> getCartByUserId(@RequestParam Long userId) {
        return ResponseEntity.ok(cartService.getCartByUserId(userId));
    }
}
