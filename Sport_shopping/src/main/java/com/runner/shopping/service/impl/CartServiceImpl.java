package com.runner.shopping.service.impl;


import com.runner.shopping.entity.Cart;
import com.runner.shopping.entity.Product;
import com.runner.shopping.entity.ProductImage;
import com.runner.shopping.entity.ProductVariant;
import com.runner.shopping.exception.InsufficientStockException;
import com.runner.shopping.exception.ResourceNotFoundException;
import com.runner.shopping.mapper.CartMapper;
import com.runner.shopping.model.dto.CartDTO;
import com.runner.shopping.repository.*;
import com.runner.shopping.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;

//    public CartServiceImpl(CartRepository cartRepository, ProductVariantRepository productVariantRepository,
//                           ProductRepository productRepository, ProductImageRepository productImageRepository,
//                           UserRepository userRepository, CartMapper cartMapper) {
//        this.cartRepository = cartRepository;
//        this.productVariantRepository = productVariantRepository;
//        this.productRepository = productRepository;
//        this.productImageRepository = productImageRepository;
//        this.userRepository = userRepository;
//        this.cartMapper = cartMapper;
//    }

    @Override
    @Transactional
    public CartDTO addToCart(CartDTO cartDTO) {
        // Kiểm tra userId
        validateUser(cartDTO.getUserId());

        // Kiểm tra variant
        ProductVariant variant = productVariantRepository.findByIdNotDeleted(cartDTO.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with id: " + cartDTO.getVariantId()));

        // Kiểm tra tồn kho
        if (variant.getStock() < cartDTO.getQuantity()) {
            throw new InsufficientStockException("Not enough stock for variant: " + cartDTO.getVariantId());
        }

        // Lấy giá
        BigDecimal price = variant.getPrice();
        cartDTO.setPriceAtTime(price);

        // Kiểm tra sản phẩm đã có trong giỏ hàng
        return cartRepository.findByUserIdAndVariantId(cartDTO.getUserId(), cartDTO.getVariantId())
                .map(cart -> {
                    int newQuantity = cart.getQuantity() + cartDTO.getQuantity();
                    if (variant.getStock() < newQuantity) {
                        throw new InsufficientStockException("Not enough stock for variant: " + cartDTO.getVariantId());
                    }
                    cart.setQuantity(newQuantity);
                    cart.setPriceAtTime(price);
                    Cart savedCart = cartRepository.save(cart);
                    return enrichCartDTO(cartMapper.toDTO(savedCart), variant);
                })
                .orElseGet(() -> {
                    Cart cart = cartMapper.toEntity(cartDTO);
                    Cart savedCart = cartRepository.save(cart);
                    return enrichCartDTO(cartMapper.toDTO(savedCart), variant);
                });
    }

    @Override
    @Transactional
    public CartDTO updateCart(Long cartId, Integer quantity) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartId));

        // Kiểm tra userId
        validateUser(cart.getUserId());

        ProductVariant variant = productVariantRepository.findByIdNotDeleted(cart.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with id: " + cart.getVariantId()));

        if (quantity > variant.getStock()) {
            throw new InsufficientStockException("Not enough stock for variant: " + cart.getVariantId());
        }

        if (quantity <= 0) {
            cartRepository.delete(cart);
            return null;
        }

        cart.setQuantity(quantity);
        cart.setPriceAtTime(variant.getPrice());
        Cart savedCart = cartRepository.save(cart);
        return enrichCartDTO(cartMapper.toDTO(savedCart), variant);
    }

    @Override
    @Transactional
    public void deleteFromCart(Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartId));

        // Kiểm tra userId
        validateUser(cart.getUserId());

        cartRepository.delete(cart);
    }

    @Override
    public List<CartDTO> getCartByUserId(Long userId) {
        // Kiểm tra userId
        validateUser(userId);

        List<Cart> carts = cartRepository.findByUserId(userId);
        return carts.stream()
                .map(cart -> {
                    ProductVariant variant = productVariantRepository.findByIdNotDeleted(cart.getVariantId())
                            .orElse(null);
                    return variant != null ? enrichCartDTO(cartMapper.toDTO(cart), variant) : null;
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    private CartDTO enrichCartDTO(CartDTO cartDTO, ProductVariant variant) {
        Product product = productRepository.findByIdNotDeleted(variant.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + variant.getProductId()));
        cartDTO.setProductName(product.getName());
        cartDTO.setSize(variant.getSize());
        cartDTO.setColor(variant.getColor());

        productImageRepository.findByProductIdNotDeleted(variant.getProductId()).stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .ifPresent(img -> cartDTO.setImageUrl(img.getImageUrl()));

        cartDTO.setTotalPrice(cartDTO.getPriceAtTime().multiply(BigDecimal.valueOf(cartDTO.getQuantity())));

        return cartDTO;
    }

    private void validateUser(Long userId) {
        userRepository.findById(userId)
//                .filter(user -> user.getRole() == UserRole.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }
}
