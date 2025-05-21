package com.runner.shopping.controller;

import com.runner.shopping.model.dto.CartDTO;
import com.runner.shopping.service.CartService;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping
    public ResponseEntity<CartDTO> addToCart(@Valid @RequestBody CartDTO cartDTO) {
        log.info("Adding to cart for userId: {}, variantId: {}", cartDTO.getUserId(), cartDTO.getVariantId());
        CartDTO addedCart = cartService.addToCart(cartDTO);
        log.info("Added to cart, cartId: {}", addedCart.getId());
        return ResponseEntity.ok(addedCart);
    }

    @PutMapping("/{cartId}")
    public ResponseEntity<CartDTO> updateCart(@PathVariable Long cartId, @RequestParam @Min(1) Integer quantity) {
        log.info("Updating cartId: {} with quantity: {}", cartId, quantity);
        CartDTO updatedCart = cartService.updateCart(cartId, quantity);
        log.info("Updated cartId: {}", cartId);
        return updatedCart != null ? ResponseEntity.ok(updatedCart) : ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{cartId}")
    public ResponseEntity<Void> deleteFromCart(@PathVariable Long cartId) {
        log.info("Deleting cartId: {}", cartId);
        cartService.deleteFromCart(cartId);
        log.info("Deleted cartId: {}", cartId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<CartDTO>> getCartByUserId(@RequestParam Long userId) {
        log.info("Fetching cart for userId: {}", userId);
        List<CartDTO> carts = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(carts);
    }
}
