package com.runner.shopping.service.impl;

import com.runner.shopping.config.VnPayConfig;
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
import com.runner.shopping.util.VnPayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
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
                .orElseThrow(() -> new ResourceNotFoundException("Địa chỉ không tìm thấy với id: " + orderDTO.getAddressId() + " cho người dùng: " + orderDTO.getUserId()));
        List<Cart> carts = cartRepository.findByUserId(orderDTO.getUserId());
        if (carts.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng trống");
        }
        BigDecimal discountPercentage = BigDecimal.ZERO;
        Promotions promotion = null;
        if (orderDTO.getPromotionId() != null) {
            promotion = promotionRepository.findByIdAndIsActiveTrue(orderDTO.getPromotionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Khuyến mãi không tìm thấy hoặc không hoạt động với id: " + orderDTO.getPromotionId()));
            LocalDateTime now = LocalDateTime.now();
            if (promotion.getStartDate() != null && now.isBefore(promotion.getStartDate())) {
                throw new IllegalArgumentException("Khuyến mãi chưa bắt đầu: " + promotion.getCode());
            }
            if (promotion.getEndDate() != null && now.isAfter(promotion.getEndDate())) {
                throw new IllegalArgumentException("Khuyến mãi đã hết hạn: " + promotion.getCode());
            }
            if (promotionUsageRepository.existsByPromotionIdAndUserId(promotion.getId(), orderDTO.getUserId())) {
                throw new IllegalArgumentException("Khuyến mãi đã được sử dụng bởi người dùng: " + promotion.getCode());
            }
            long usageCount = promotionUsageRepository.countByPromotionId(promotion.getId());
            if (promotion.getMaxUsage() != null && usageCount >= promotion.getMaxUsage()) {
                throw new IllegalArgumentException("Khuyến mãi đã đạt giới hạn sử dụng: " + promotion.getCode());
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
                    ProductVariant variant = productVariantRepository.findByIdNotDeletedWithLock(cart.getVariantId())
                            .orElseThrow(() -> new ResourceNotFoundException("Biến thể sản phẩm không tìm thấy với id: " + cart.getVariantId()));
                    if (variant.getStock() < cart.getQuantity()) {
                        throw new InsufficientStockException("Sản phẩm " + cart.getVariantId() + " không đủ hàng trong kho");
                    }
                    if (!cart.getPriceAtTime().equals(variant.getPrice())) {
                        throw new IllegalStateException("Giá sản phẩm " + cart.getVariantId() + " đã thay đổi. Vui lòng làm mới giỏ hàng.");
                    }
                    variant.setStock(variant.getStock() - cart.getQuantity());
                    productVariantRepository.save(variant);
                    InventoryLogs log = new InventoryLogs();
                    log.setVariantId(cart.getVariantId());
                    log.setQuantityChange(-cart.getQuantity());
                    log.setReason("Đặt hàng cho đơn hàng ID: " + savedOrder.getId());
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
            throw new IllegalArgumentException("Tổng giá trị đơn hàng không đạt mức tối thiểu để áp dụng khuyến mãi: " + promotion.getCode());
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
        payment.setPaymentMethod(orderDTO.getPaymentMethod()); // Đã sửa: Lấy paymentMethod từ orderDTO
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);
        cartRepository.deleteByUserId(orderDTO.getUserId());
        OrderDTO result = orderMapper.toDTO(finalOrder);
        result.setPaymentMethod(orderDTO.getPaymentMethod()); // Thêm: Gán paymentMethod vào DTO
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
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tìm thấy với id: " + id + " cho người dùng: " + userId));
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
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tìm thấy với id: " + orderId));
        if (!order.getStatus().equals(OrderStatus.PENDING) && !order.getStatus().equals(OrderStatus.CONFIRMED)) {
            throw new IllegalStateException("Chỉ có thể hủy đơn hàng ở trạng thái PENDING hoặc CONFIRMED");
        }
        if (order.getCreatedAt().isBefore(LocalDateTime.now().minusHours(24))) {
            throw new IllegalStateException("Đơn hàng chỉ có thể hủy trong vòng 24 giờ sau khi tạo");
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.CANCELLED);
        orderRepository.save(order);
        if (order.getPromotionId() != null) {
            promotionUsageRepository.deleteByOrderId(orderId);
        }
        List<OrderDetails> details = orderDetailRepository.findByOrderId(orderId);
        for (OrderDetails detail : details) {
            ProductVariant variant = productVariantRepository.findByIdNotDeletedWithLock(detail.getVariantId())
                    .orElse(null);
            if (variant != null) {
                variant.setStock(variant.getStock() + detail.getQuantity());
                productVariantRepository.save(variant);
                InventoryLogs log = new InventoryLogs();
                log.setVariantId(detail.getVariantId());
                log.setQuantityChange(detail.getQuantity());
                log.setReason("Hủy đơn hàng với ID: " + orderId);
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
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tìm thấy với id: " + orderId));
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

    @Override
    @Transactional
    public String initiateVNPayPayment(Long orderId, Long userId, String returnUrl) {
        Orders order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tìm thấy với id: " + orderId));
        List<Payments> payments = paymentRepository.findAllByOrderId(orderId);
        if (payments.isEmpty()) {
            throw new ResourceNotFoundException("Thanh toán không tìm thấy cho đơn hàng id: " + orderId);
        }
        if (payments.size() > 1) {
            log.warn("Multiple payment records found for orderId={}. Using the first one.", orderId);
        }
        Payments payment = payments.get(0); // Lấy bản ghi đầu tiên
        if (!payment.getStatus().equals(PaymentStatus.PENDING)) {
            throw new IllegalStateException("Thanh toán không ở trạng thái PENDING");
        }
        if (!payment.getPaymentMethod().equals(PaymentMethod.VNPAY)) {
            throw new IllegalStateException("Phương thức thanh toán không phải VNPay");
        }
        String vnpayUrl = createVNPayPaymentUrl(order, payment, returnUrl);
        String vnp_IpAddr = getClientIp();
        payment.setClientIp(vnp_IpAddr != null ? vnp_IpAddr : "127.0.0.1");
        paymentRepository.save(payment);
        return vnpayUrl;
    }

    private String createVNPayPaymentUrl(Orders order, Payments payment, String returnUrl) {
        try {
            String vnp_Version = "2.1.0";
            String vnp_Command = "pay";
            String vnp_TxnRef = order.getId() + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String vnp_Amount = String.valueOf(payment.getAmount().multiply(new BigDecimal(100)).longValueExact());
            String vnp_CurrCode = "VND";
            String vnp_IpAddr = getClientIp();
            String vnp_CreateDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String vnp_Locale = "vn";
            String vnp_OrderInfo = "Thanh toan don hang " + order.getId();
            String vnp_OrderType = "fashion";
            String vnp_ReturnUrl = returnUrl; // Không mã hóa trước, để hashAllFields xử lý

            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", vnp_Version);
            vnp_Params.put("vnp_Command", vnp_Command);
            vnp_Params.put("vnp_TmnCode", VnPayConfig.VNP_TMN_CODE);
            vnp_Params.put("vnp_Amount", vnp_Amount);
            vnp_Params.put("vnp_CurrCode", vnp_CurrCode);
            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
            vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
            vnp_Params.put("vnp_OrderType", vnp_OrderType);
            vnp_Params.put("vnp_Locale", vnp_Locale);
            vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
            vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

            log.info("VNPay Parameters before hashing: {}", vnp_Params);

            String vnp_SecureHash = VnPayUtil.hashAllFields(vnp_Params);
            vnp_Params.put("vnp_SecureHash", vnp_SecureHash);
            log.info("Generated VNPay Payment URL: {}", vnp_SecureHash);

            StringBuilder query = new StringBuilder();
            vnp_Params.forEach((key, value) -> {
                try {
                    // Mã hóa lại cho URL, nhưng giữ %20 để tương thích với trình duyệt
                    query.append(URLEncoder.encode(key, StandardCharsets.UTF_8.toString()))
                            .append("=")
                            .append(URLEncoder.encode(value, StandardCharsets.UTF_8.toString()))
                            .append("&");
                } catch (Exception e) {
                    throw new RuntimeException("Lỗi khi mã hóa tham số VNPay", e);
                }
            });
            String queryUrl = query.substring(0, query.length() - 1);
            String paymentUrl = VnPayConfig.VNP_PAY_URL + "?" + queryUrl;
            log.info("Generated VNPay Payment URL: {}", paymentUrl);
            return paymentUrl;
        } catch (Exception e) {
            log.error("Error creating VNPay/return URL: {}", e);
            throw new RuntimeException("Error creating payment URL", e);
        }
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                String ip = attributes.getRequest().getRemoteAddr();
                if (ip.equals("0:0:0:0:0:0:0:1")) {
                    return "127.0.0.1"; // Chuyển IPv6 localhost thành IPv4
                }
                return ip;
            }
            return "127.0.0.1";
        } catch (Exception e) {
            return "127.0.0.1";
        }
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
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tìm thấy hoặc không phải khách hàng với id: " + userId));
    }

    private void validateStaff(Long staffId) {
        userRepository.findById(staffId)
                .filter(user -> user.getRole() == UserRole.STAFF || user.getRole() == UserRole.ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên không tìm thấy với id: " + staffId));
    }
}