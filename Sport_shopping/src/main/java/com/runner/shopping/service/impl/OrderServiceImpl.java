package com.runner.shopping.service.impl;


import com.runner.shopping.entity.*;
import com.runner.shopping.entity.*;
import com.runner.shopping.enums.OrderStatus;
import com.runner.shopping.enums.PaymentMethod;
import com.runner.shopping.enums.PaymentStatus;
import com.runner.shopping.enums.UserRole;
import com.runner.shopping.exception.InsufficientStockException;
import com.runner.shopping.exception.ResourceNotFoundException;
import com.runner.shopping.mapper.OrderMapper;
import com.runner.shopping.model.dto.OrderDTO;
import com.runner.shopping.repository.*;
import com.runner.shopping.service.OrderService;
import lombok.RequiredArgsConstructor;
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
    private final InventoryLogRepository inventoryLogRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final PromotionRepository promotionRepository;
    private final PromotionUsageRepository promotionUsageRepository;
    private final PaymentRepository paymentRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderDTO createOrder(OrderDTO orderDTO) {
        // Kiểm tra userId
        validateUser(orderDTO.getUserId());

        // Kiểm tra addressId
        Addresses address = addressRepository.findByIdAndUserId(orderDTO.getAddressId(), orderDTO.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + orderDTO.getAddressId() + " for user: " + orderDTO.getUserId()));

        // Kiểm tra giỏ hàng
        List<Cart> carts = cartRepository.findByUserId(orderDTO.getUserId());
        if (carts.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        // Kiểm tra promotionId (nếu có)
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

        // Tạo đơn hàng
        Orders order = orderMapper.toEntity(orderDTO);
        order.setStatus(OrderStatus.PENDING); // Gán rõ ràng status
        order.setPaymentStatus(PaymentStatus.PENDING); // Gán rõ ràng paymentStatus
        order.setTotalPrice(BigDecimal.ZERO);
        Orders savedOrder = orderRepository.save(order);

        // Tạo chi tiết đơn hàng từ giỏ hàng
        List<OrderDetails> orderDetails = carts.stream()
                .map(cart -> {
                    ProductVariant variant = productVariantRepository.findByIdNotDeleted(cart.getVariantId())
                            .orElseThrow(() -> new ResourceNotFoundException("Product variant not found with id: " + cart.getVariantId()));

                    // Kiểm tra tồn kho
                    if (variant.getStock() < cart.getQuantity()) {
                        throw new InsufficientStockException("Not enough stock for variant: " + cart.getVariantId());
                    }

                    // Giảm tồn kho
                    variant.setStock(variant.getStock() - cart.getQuantity());
                    productVariantRepository.save(variant);

                    // Ghi log tồn kho
                    InventoryLogs log = new InventoryLogs();
                    log.setVariantId(cart.getVariantId());
                    log.setQuantityChange(-cart.getQuantity());
                    log.setReason("Order placed for order ID: " + savedOrder.getId());
                    log.setCreatedBy(null); // Hệ thống thực hiện
                    inventoryLogRepository.save(log);

                    // Tạo chi tiết đơn hàng
                    OrderDetails detail = new OrderDetails();
                    detail.setOrderId(savedOrder.getId());
                    detail.setVariantId(cart.getVariantId());
                    detail.setQuantity(cart.getQuantity());
                    detail.setPriceAtTime(variant.getPrice());
                    return detail;
                })
                .collect(Collectors.toList());

        // Lưu chi tiết đơn hàng
        orderDetailRepository.saveAll(orderDetails);

        // Tính tổng tiền
        BigDecimal subTotal = orderDetails.stream()
                .map(detail -> detail.getPriceAtTime().multiply(BigDecimal.valueOf(detail.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (promotion != null && promotion.getMinimumOrderValue() != null && subTotal.compareTo(promotion.getMinimumOrderValue()) < 0) {
            throw new IllegalArgumentException("Order total is below minimum required for promotion: " + promotion.getCode());
        }
        BigDecimal discount = subTotal.multiply(discountPercentage).divide(BigDecimal.valueOf(100));
        BigDecimal totalPrice = subTotal.subtract(discount);
        savedOrder.setTotalPrice(totalPrice);
        orderRepository.save(savedOrder);

        // Ghi PromotionUsage (nếu có)
        if (promotion != null) {
            PromotionUsage usage = new PromotionUsage();
            usage.setPromotionId(promotion.getId());
            usage.setUserId(orderDTO.getUserId());
            usage.setOrderId(savedOrder.getId());
            promotionUsageRepository.save(usage);
        }

        // Tạo bản ghi Payments
        Payments payment = new Payments();
        payment.setOrderId(savedOrder.getId());
        payment.setAmount(totalPrice);
        payment.setPaymentMethod(orderDTO.getPaymentMethod());
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        // KHÔNG xóa giỏ hàng ở đây, chờ thanh toán thành công
        OrderDTO result = orderMapper.toDTO(savedOrder);
        result.setOrderDetails(orderMapper.toDetailDTOList(orderDetailRepository.findByOrderId(savedOrder.getId())));
        return result;
    }

    @Override
    public OrderDTO getOrderById(Long id, Long userId) {
        Orders order = orderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id + " for user: " + userId));
        OrderDTO orderDTO = orderMapper.toDTO(order);
        orderDTO.setOrderDetails(orderMapper.toDetailDTOList(orderDetailRepository.findByOrderId(id)));
        return orderDTO;
    }

    @Override
    public List<OrderDTO> getOrdersByUserId(Long userId) {
        validateUser(userId);
        List<Orders> orders = orderRepository.findByUserId(userId);
        List<OrderDTO> orderDTOs = orderMapper.toDTOList(orders);
        orderDTOs.forEach(dto -> dto.setOrderDetails(
                orderMapper.toDetailDTOList(orderDetailRepository.findByOrderId(dto.getId()))
        ));
        return orderDTOs;
    }

    private void validateUser(Long userId) {
        userRepository.findById(userId)
                .filter(user -> user.getRole() == UserRole.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("User not found or not a customer with id: " + userId));
    }
}
