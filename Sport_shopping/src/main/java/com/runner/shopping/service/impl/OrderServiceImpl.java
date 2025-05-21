package com.runner.shopping.service.impl;

import com.runner.shopping.entity.*;
import com.runner.shopping.enums.OrderStatus;
import com.runner.shopping.enums.PaymentMethod;
import com.runner.shopping.enums.PaymentStatus;
import com.runner.shopping.enums.UserRole;
import com.runner.shopping.exception.InsufficientStockException;
import com.runner.shopping.exception.ResourceNotFoundException;
import com.runner.shopping.mapper.AddressMapper;
import com.runner.shopping.mapper.OrderMapper;
import com.runner.shopping.model.dto.OrderDTO;
import com.runner.shopping.model.dto.OrderDetailDTO;
import com.runner.shopping.repository.*;
import com.runner.shopping.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final PaymentRepository paymentRepository;
    private final OrderMapper orderMapper;
    private final AddressMapper addressMapper;

    @Override
    @Transactional
    public OrderDTO createOrder(OrderDTO orderDTO) {
        validateUser(orderDTO.getUserId());
        Addresses address = addressRepository.findByIdAndUserId(orderDTO.getAddressId(), orderDTO.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + orderDTO.getAddressId() + " for user: " + orderDTO.getUserId()));
        List<Cart> carts = cartRepository.findByUserId(orderDTO.getUserId());
        if (carts.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }
        BigDecimal discountPercentage = BigDecimal.ZERO;
        Promotions promotion = null;
        if (orderDTO.getPromotionId() != null) {
            promotion = promotionRepository.findByIdAndIsActiveTrue(orderDTO.getPromotionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Promotion not found or inactive with id: " + orderDTO.getPromotionId()));
            LocalDateTime now = LocalDateTime.now();
            if (promotion.getStartDate() != null && now.isBefore(promotion.getStartDate())) {
                throw new IllegalArgumentException("Promotion not yet started: " + promotion.getCode());
            }
            if (promotion.getEndDate() != null && now.isAfter(promotion.getEndDate())) {
                throw new IllegalArgumentException("Promotion expired: " + promotion.getCode());
            }
            if (promotionUsageRepository.existsByPromotionIdAndUserId(promotion.getId(), orderDTO.getUserId())) {
                throw new IllegalArgumentException("Promotion already used by user: " + promotion.getCode());
            }
            long usageCount = promotionUsageRepository.countByPromotionId(promotion.getId());
            if (promotion.getMaxUsage() != null && usageCount >= promotion.getMaxUsage()) {
                throw new IllegalArgumentException("Promotion usage limit reached: " + promotion.getCode());
            }
            discountPercentage = promotion.getDiscountPercentage();
        }
        Orders order = orderMapper.toEntity(orderDTO);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setTotalPrice(BigDecimal.ZERO);
        Orders savedOrder = orderRepository.save(order);
        List<OrderDetails> orderDetails = carts.stream()
                .map(cart -> {
                    ProductVariant variant = productVariantRepository.findByIdNotDeleted(cart.getVariantId())
                            .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with id: " + cart.getVariantId()));
                    if (variant.getStock() < cart.getQuantity()) {
                        throw new InsufficientStockException("Not enough stock for variant: " + cart.getVariantId());
                    }
                    variant.setStock(variant.getStock() - cart.getQuantity());
                    productVariantRepository.save(variant);
                    InventoryLogs log = new InventoryLogs();
                    log.setVariantId(cart.getVariantId());
                    log.setQuantityChange(-cart.getQuantity());
                    log.setReason("Order placed for order ID: " + savedOrder.getId());
                    log.setCreatedBy(null);
                    inventoryLogRepository.save(log);
                    OrderDetails detail = new OrderDetails();
                    detail.setOrderId(savedOrder.getId());
                    detail.setVariantId(cart.getVariantId());
                    detail.setQuantity(cart.getQuantity());
                    detail.setPriceAtTime(variant.getPrice());
                    return detail;
                })
                .collect(Collectors.toList());
        orderDetailRepository.saveAll(orderDetails);
        BigDecimal subTotal = orderDetails.stream()
                .map(detail -> detail.getPriceAtTime().multiply(BigDecimal.valueOf(detail.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (promotion != null && promotion.getMinimumOrderValue() != null && subTotal.compareTo(promotion.getMinimumOrderValue()) < 0) {
            throw new IllegalArgumentException("Order total is below minimum required for promotion: " + promotion.getCode());
        }
        BigDecimal discount = subTotal.multiply(discountPercentage).divide(BigDecimal.valueOf(100));
        BigDecimal totalPrice = subTotal.subtract(discount);
        savedOrder.setTotalPrice(totalPrice);
        Orders finalOrder = orderRepository.save(savedOrder);
        if (promotion != null) {
            PromotionUsage usage = new PromotionUsage();
            usage.setPromotionId(promotion.getId());
            usage.setUserId(orderDTO.getUserId());
            usage.setOrderId(savedOrder.getId());
            promotionUsageRepository.save(usage);
        }
        Payments payment = new Payments();
        payment.setOrderId(savedOrder.getId());
        payment.setAmount(totalPrice);
        payment.setPaymentMethod(orderDTO.getPaymentMethod());
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);
        OrderDTO result = orderMapper.toDTO(finalOrder);
        result.setOrderDetails(enrichOrderDetails(orderDetailRepository.findByOrderId(savedOrder.getId())));
        result.setAddressDetails(addressRepository.findById(finalOrder.getAddressId())
                .map(addressMapper::toDTO).orElse(null));
        if (finalOrder.getPromotionId() != null) {
            promotionRepository.findById(finalOrder.getPromotionId())
                    .ifPresent(p -> result.setPromotionCode(p.getCode()));
        }
        result.setCreatedAt(finalOrder.getCreatedAt());
        return result;
    }

    @Override
    @Transactional
    public OrderDTO getOrderById(Long id, Long userId) {
        Orders order = orderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id + " for user: " + userId));
        OrderDTO orderDTO = orderMapper.toDTO(order);
        orderDTO.setOrderDetails(enrichOrderDetails(orderDetailRepository.findByOrderId(id)));
        orderDTO.setAddressDetails(addressRepository.findById(order.getAddressId())
                .map(addressMapper::toDTO).orElse(null));
        if (order.getPromotionId() != null) {
            promotionRepository.findById(order.getPromotionId())
                    .ifPresent(p -> orderDTO.setPromotionCode(p.getCode()));
        }
        orderDTO.setCreatedAt(order.getCreatedAt());
        return orderDTO;
    }

    @Override
    @Transactional
    public List<OrderDTO> getOrdersByUserId(Long userId) {
        validateUser(userId);
        List<Orders> orders = orderRepository.findByUserId(userId);
        return orders.stream()
                .map(order -> {
                    OrderDTO dto = orderMapper.toDTO(order);
                    dto.setOrderDetails(enrichOrderDetails(orderDetailRepository.findByOrderId(dto.getId())));
                    dto.setAddressDetails(addressRepository.findById(order.getAddressId())
                            .map(addressMapper::toDTO).orElse(null));
                    if (order.getPromotionId() != null) {
                        promotionRepository.findById(order.getPromotionId())
                                .ifPresent(p -> dto.setPromotionCode(p.getCode()));
                    }
                    dto.setCreatedAt(order.getCreatedAt());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Orders order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        if (!order.getStatus().equals(OrderStatus.PENDING)) {
            throw new IllegalStateException("Only pending orders can be cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.CANCELLED);
        orderRepository.save(order);
        List<OrderDetails> details = orderDetailRepository.findByOrderId(orderId);
        for (OrderDetails detail : details) {
            ProductVariant variant = productVariantRepository.findByIdNotDeleted(detail.getVariantId())
                    .orElse(null);
            if (variant != null) {
                variant.setStock(variant.getStock() + detail.getQuantity());
                productVariantRepository.save(variant);
                InventoryLogs log = new InventoryLogs();
                log.setVariantId(detail.getVariantId());
                log.setQuantityChange(detail.getQuantity());
                log.setReason("Order cancelled for order ID: " + orderId);
                log.setCreatedBy(userId);
                inventoryLogRepository.save(log);
            }
        }
    }

    @Override
    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, OrderStatus newStatus, Long staffId) {
        validateStaff(staffId);
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        order.setStatus(newStatus);
        order.setHandledBy(staffId);
        Orders savedOrder = orderRepository.save(order);
        OrderDTO result = orderMapper.toDTO(savedOrder);
        result.setOrderDetails(enrichOrderDetails(orderDetailRepository.findByOrderId(orderId)));
        result.setAddressDetails(addressRepository.findById(savedOrder.getAddressId())
                .map(addressMapper::toDTO).orElse(null));
        if (savedOrder.getPromotionId() != null) {
            promotionRepository.findById(savedOrder.getPromotionId())
                    .ifPresent(p -> result.setPromotionCode(p.getCode()));
        }
        result.setCreatedAt(savedOrder.getCreatedAt());
        return result;
    }

    @Override
    @Transactional
    public Page<OrderDTO> getAllOrders(Long staffId, OrderStatus status, Long userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        validateStaff(staffId);
        Page<Orders> ordersPage = orderRepository.findOrdersWithFilters(status, userId, startDate, endDate, pageable);
        List<OrderDTO> orderDTOs = ordersPage.getContent().stream()
                .map(order -> {
                    OrderDTO dto = orderMapper.toDTO(order);
                    dto.setOrderDetails(enrichOrderDetails(orderDetailRepository.findByOrderId(dto.getId())));
                    dto.setAddressDetails(addressRepository.findById(order.getAddressId())
                            .map(addressMapper::toDTO).orElse(null));
                    if (order.getPromotionId() != null) {
                        promotionRepository.findById(order.getPromotionId())
                                .ifPresent(p -> dto.setPromotionCode(p.getCode()));
                    }
                    dto.setCreatedAt(order.getCreatedAt());
                    return dto;
                })
                .collect(Collectors.toList());
        return new PageImpl<>(orderDTOs, pageable, ordersPage.getTotalElements());
    }

    private List<OrderDetailDTO> enrichOrderDetails(List<OrderDetails> details) {
        return details.stream()
                .map(detail -> {
                    OrderDetailDTO dto = orderMapper.toDetailDTO(detail);
                    ProductVariant variant = productVariantRepository.findByIdNotDeleted(detail.getVariantId())
                            .orElse(null);
                    if (variant != null) {
                        Product product = productRepository.findByIdNotDeleted(variant.getProductId())
                                .orElse(null);
                        if (product != null) {
                            dto.setProductName(product.getName());
                            dto.setSize(variant.getSize());
                            dto.setColor(variant.getColor());
                            productImageRepository.findByProductIdNotDeleted(variant.getProductId()).stream()
                                    .filter(ProductImage::isPrimary)
                                    .findFirst()
                                    .ifPresent(img -> dto.setImageUrl(img.getImageUrl()));
                        }
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private void validateUser(Long userId) {
        userRepository.findById(userId)
                .filter(user -> user.getRole() == UserRole.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("User not found or not a customer with id: " + userId));
    }

    private void validateStaff(Long staffId) {
        userRepository.findById(staffId)
                .filter(user -> user.getRole() == UserRole.STAFF || user.getRole() == UserRole.ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found with id: " + staffId));
    }
}