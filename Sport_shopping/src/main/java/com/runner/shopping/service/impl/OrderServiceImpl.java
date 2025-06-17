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
    private final PaymentRepository paymentsRepository;

    @Override
    @Transactional
    public OrderDTO createOrder(OrderDTO orderDTO) {
        // 1. Validate user và địa chỉ
        validateUser(orderDTO.getUserId());
        Addresses address = addressRepository.findByIdAndUserId(orderDTO.getAddressId(), orderDTO.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Địa chỉ không tìm thấy với id: " + orderDTO.getAddressId() + " cho người dùng: " + orderDTO.getUserId()));

        // 2. Lấy giỏ hàng của user
        List<Cart> carts = cartRepository.findByUserId(orderDTO.getUserId());
        if (carts.isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng trống");
        }

        // 3. Xác định khuyến mãi (nếu có) và lấy discountPercentage
        BigDecimal discountPercentage = BigDecimal.ZERO;
        Promotions promotion = null;
        if (orderDTO.getPromotionId() != null) {
            promotion = promotionRepository.findByIdAndIsActiveTrue(orderDTO.getPromotionId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Khuyến mãi không tìm thấy hoặc không hoạt động với id: " + orderDTO.getPromotionId()));
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

        // 4. Tạo entity Orders từ DTO (MapStruct), set tạm totalPrice = 0, totalCost = 0, totalProfit = 0
        Orders order = orderMapper.toEntity(orderDTO);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);

        // Khởi tạo totalPrice/totalCost/totalProfit = 0; service sẽ gán lại sau
        order.setTotalPrice(BigDecimal.ZERO);
        order.setTotalCost(BigDecimal.ZERO);
        order.setTotalProfit(BigDecimal.ZERO);

        Orders savedOrder = orderRepository.save(order);

        // 5. Tạo list<OrderDetails> dựa vào Cart
        List<OrderDetails> orderDetails = carts.stream()
                .map(cart -> {
                    // a. Lấy variant với lock để tránh race condition
                    ProductVariant variant = productVariantRepository.findByIdNotDeletedWithLock(cart.getVariantId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "Biến thể sản phẩm không tìm thấy với id: " + cart.getVariantId()));

                    // b. Kiểm tra tồn kho
                    if (variant.getStock() < cart.getQuantity()) {
                        throw new InsufficientStockException(
                                "Sản phẩm " + cart.getVariantId() + " không đủ hàng trong kho");
                    }

                    // c. Kiểm tra giá tại giỏ có trùng với giá hiện tại chưa
                    if (!cart.getPriceAtTime().equals(variant.getPrice())) {
                        throw new IllegalStateException(
                                "Giá sản phẩm " + cart.getVariantId() + " đã thay đổi. Vui lòng làm mới giỏ hàng.");
                    }

                    // d. Trừ kho
                    variant.setStock(variant.getStock() - cart.getQuantity());
                    productVariantRepository.save(variant);

                    // e. Ghi inventory log
                    InventoryLogs logEntry = new InventoryLogs();
                    logEntry.setVariantId(cart.getVariantId());
                    logEntry.setQuantityChange(-cart.getQuantity());
                    logEntry.setReason("Đặt hàng cho đơn hàng ID: " + savedOrder.getId());
                    logEntry.setCreatedBy(null); // có thể gán userId nếu muốn
                    inventoryLogRepository.save(logEntry);

                    // f. Tạo OrderDetails (đã có các trường mới costAtTime & priceAtTime)
                    OrderDetails detail = new OrderDetails();
                    detail.setOrderId(savedOrder.getId());
                    detail.setVariantId(cart.getVariantId());
                    detail.setQuantity(cart.getQuantity());
                    detail.setPriceAtTime(variant.getPrice());
                    detail.setCostAtTime(variant.getCostPrice()); // gán giá vốn tại thời điểm

                    return detail;
                })
                .collect(Collectors.toList());

        // 6. Lưu tất cả OrderDetails
        orderDetailRepository.saveAll(orderDetails);

        // 7. Tính subTotal (tổng doanh thu chưa trừ khuyến mãi) và totalCost (tổng giá vốn)
        BigDecimal subTotal = orderDetails.stream()
                .map(d -> d.getPriceAtTime().multiply(BigDecimal.valueOf(d.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCost = orderDetails.stream()
                .map(d -> d.getCostAtTime().multiply(BigDecimal.valueOf(d.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 8. Kiểm tra điều kiện minOrderValue (nếu có khuyến mãi)
        if (promotion != null && promotion.getMinimumOrderValue() != null
                && subTotal.compareTo(promotion.getMinimumOrderValue()) < 0) {
            throw new IllegalArgumentException(
                    "Tổng giá trị đơn hàng không đạt mức tối thiểu để áp dụng khuyến mãi: " + promotion.getCode());
        }

        // 9A. Tính discount dựa trên discountAmount hoặc discountPercentage
        BigDecimal discount = BigDecimal.ZERO;
        if (promotion != null) {
            // 9.1. Ưu tiên discountAmount nếu > 0
            if (promotion.getDiscountAmount() != null
                    && promotion.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                discount = promotion.getDiscountAmount();
            }
            // 9.2. Ngược lại nếu có discountPercentage thì tính %
            else if (promotion.getDiscountPercentage() != null
                    && promotion.getDiscountPercentage().compareTo(BigDecimal.ZERO) > 0) {
                discount = subTotal
                        .multiply(promotion.getDiscountPercentage())
                        .divide(BigDecimal.valueOf(100));
            }
            // 9.3. Đảm bảo discount không vượt quá subTotal
            if (discount.compareTo(subTotal) > 0) {
                discount = subTotal;
            }
        }

        // 9B. Tính tổng giá sau khi trừ khuyến mãi
        BigDecimal totalPrice = subTotal.subtract(discount);

        // 10. Tính tổng lợi nhuận = totalPrice – totalCost
        BigDecimal totalProfit = totalPrice.subtract(totalCost);

        // 11. Gán vào đối tượng Orders và lưu lại
        savedOrder.setTotalPrice(totalPrice);
        savedOrder.setTotalCost(totalCost);
        savedOrder.setTotalProfit(totalProfit);
        Orders finalOrder = orderRepository.save(savedOrder);

        // 12. Ghi PromotionUsage (nếu sử dụng)
        if (promotion != null) {
            PromotionUsage usage = new PromotionUsage();
            usage.setPromotionId(promotion.getId());
            usage.setUserId(orderDTO.getUserId());
            usage.setOrderId(finalOrder.getId());
            promotionUsageRepository.save(usage);
        }

        // 13. Tạo Payments record với số tiền = totalPrice
        Payments payment = new Payments();
        payment.setOrderId(finalOrder.getId());
        payment.setAmount(totalPrice);
        payment.setPaymentMethod(orderDTO.getPaymentMethod());
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        // 14. Xóa toàn bộ giỏ hàng (Cart) của user
        cartRepository.deleteByUserId(orderDTO.getUserId());

        // 15. Map lại OrderDTO để trả về client
        OrderDTO result = orderMapper.toDTO(finalOrder);
        result.setPaymentMethod(orderDTO.getPaymentMethod());
        result.setOrderDetails(enrichOrderDetails(orderDetailRepository.findByOrderId(finalOrder.getId())));
        result.setAddressDetails(
                addressRepository.findById(finalOrder.getAddressId()).map(addressMapper::toDTO).orElse(null));
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
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Đơn hàng không tìm thấy với id: " + id + " cho người dùng: " + userId));

        OrderDTO orderDTO = orderMapper.toDTO(order);
        // order details
        orderDTO.setOrderDetails(
                enrichOrderDetails(orderDetailRepository.findByOrderId(id))
        );
        // address
        orderDTO.setAddressDetails(
                addressRepository.findById(order.getAddressId())
                        .map(addressMapper::toDTO)
                        .orElse(null)
        );
        // promotion code
        if (order.getPromotionId() != null) {
            promotionRepository.findById(order.getPromotionId())
                    .ifPresent(p -> orderDTO.setPromotionCode(p.getCode()));
        }
        // **lấy paymentMethod**
        Payments payment = paymentRepository
                .findFirstByOrderIdOrderByCreatedAtDesc(order.getId());
        if (payment != null) {
            orderDTO.setPaymentMethod(payment.getPaymentMethod());
        }
        // createdAt (nếu bạn muốn ghi đè lại)
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

        // Chỉ cho phép hủy khi PENDING hoặc CONFIRMED (chưa ship/delivered)
        if (!order.getStatus().equals(OrderStatus.PENDING)
                && !order.getStatus().equals(OrderStatus.CONFIRMED)) {
            throw new IllegalStateException("Chỉ có thể hủy đơn hàng ở trạng thái PENDING hoặc CONFIRMED");
        }
        // Chỉ cho hủy trong vòng 24 giờ
        if (order.getCreatedAt().isBefore(LocalDateTime.now().minusHours(24))) {
            throw new IllegalStateException("Đơn hàng chỉ có thể hủy trong vòng 24 giờ sau khi tạo");
        }

        // 1. Thay đổi trạng thái order và payment, set canceledAt
        order.setStatus(OrderStatus.CANCELLED);
        order.setCanceledAt(LocalDateTime.now());

        // Lấy record payment liên quan (nếu có)
        Payments payment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (payment != null) {
            // Nếu payment chưa được hoàn tiền, set trạng thái REFUNDED
            if (payment.getStatus().equals(PaymentStatus.COMPLETED)
                    || payment.getStatus().equals(PaymentStatus.PENDING)) {
                payment.setStatus(PaymentStatus.REFUNDED);
                payment.setRefundAmount(payment.getAmount());       // Nếu bạn muốn lưu lại số tiền đã hoàn
                payment.setRefundedAt(LocalDateTime.now());         // Thời điểm hoàn tiền
                paymentRepository.save(payment);
            } else {
                // Nếu payment còn ở status khác (FAILED/CANCELLED), giữ nguyên
            }
        }

        // 2. Xóa bản ghi PromotionUsage nếu có
        if (order.getPromotionId() != null) {
            promotionUsageRepository.deleteByOrderId(orderId);
        }

        // 3. Trả lại tồn kho dựa trên OrderDetails
        List<OrderDetails> details = orderDetailRepository.findByOrderId(orderId);
        for (OrderDetails detail : details) {
            ProductVariant variant = productVariantRepository.findByIdNotDeletedWithLock(detail.getVariantId())
                    .orElse(null);
            if (variant != null) {
                variant.setStock(variant.getStock() + detail.getQuantity());
                productVariantRepository.save(variant);

                // Ghi log trả kho
                InventoryLogs log = new InventoryLogs();
                log.setVariantId(detail.getVariantId());
                log.setQuantityChange(detail.getQuantity());
                log.setReason("Hủy đơn hàng với ID: " + orderId);
                log.setCreatedBy(userId);
                inventoryLogRepository.save(log);
            }
        }

        // 4. Lưu order đã cập nhật
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public OrderDTO updateOrderStatus(Long orderId, OrderStatus newStatus, Long staffId) {
        // 1. Xác thực nhân viên
        validateStaff(staffId);

        // 2. Lấy Order hiện tại (entity managed)
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Đơn hàng không tìm thấy với id: " + orderId));

        // 3. Lấy bản ghi Payment mới nhất để biết paymentMethod
        Payments latestPay = paymentRepository
                .findFirstByOrderIdOrderByCreatedAtDesc(orderId);
        PaymentMethod payMethod = (latestPay != null)
                ? latestPay.getPaymentMethod() : null;

        // 4. Lấy trạng thái thanh toán hiện tại từ Order
        PaymentStatus currentPay = order.getPaymentStatus();

        // 5. Tính trạng thái thanh toán kế tiếp theo rule
        PaymentStatus nextPay = resolveNextPaymentStatus(newStatus, payMethod, currentPay);

        // 6. Cập nhật trạng thái đơn và người xử lý
        order.setStatus(newStatus);
        order.setHandledBy(staffId);

        // 7. Nếu hủy đơn, ghi thời điểm hủy
        if (newStatus == OrderStatus.CANCELLED) {
            order.setCanceledAt(LocalDateTime.now());
        }

        // 8. Nếu cần đổi trạng thái thanh toán trên Order + Payment
        if (nextPay != null && nextPay != currentPay) {
            // 8.1. Cập nhật trên Orders
            order.setPaymentStatus(nextPay);

            // 8.2. Đồng bộ lên bảng Payments
            if (latestPay != null) {
                BigDecimal refundAmt = null;
                LocalDateTime refundedAt = null;

                // Nếu trạng thái REFUNDED thì set refundAmount + refundedAt
                if (nextPay == PaymentStatus.REFUNDED) {
                    refundAmt  = order.getTotalPrice();
                    refundedAt = LocalDateTime.now();
                }

                paymentRepository.updateStatusAndRefund(
                        latestPay.getId(),
                        nextPay,
                        refundAmt,
                        refundedAt
                );
            }
        }

        // 9. Lưu lại Order (flush tất cả thay đổi)
        Orders saved = orderRepository.save(order);

        // 10. Build và trả về DTO (MapStruct + enrich)
        return buildDto(saved);
    }

    /**
     * Ánh xạ OrderStatus → PaymentStatus theo nghiệp vụ:
     * - DELIVERED + COD      → COMPLETED
     * - DELIVERED + VNPAY    → giữ nguyên (COMPLETED từ trước)
     * - CANCELLED + VNPAY    → REFUNDED
     * - CANCELLED + (COD)    → CANCELLED
     * - Các trạng thái khác  → null (không đổi)
     */
    private PaymentStatus resolveNextPaymentStatus(
            OrderStatus orderSt,
            PaymentMethod payMethod,
            PaymentStatus currentPay) {

        if (orderSt == OrderStatus.DELIVERED
                && payMethod == PaymentMethod.CASH_ON_DELIVERY
                && currentPay != PaymentStatus.COMPLETED) {
            return PaymentStatus.COMPLETED;
        }

        if (orderSt == OrderStatus.CANCELLED) {
            if (payMethod == PaymentMethod.VNPAY
                    && currentPay != PaymentStatus.REFUNDED) {
                return PaymentStatus.REFUNDED;
            }
            if (payMethod != PaymentMethod.VNPAY
                    && currentPay != PaymentStatus.CANCELLED) {
                return PaymentStatus.CANCELLED;
            }
        }

        return null;
    }

    /**
     * Tạo OrderDTO, bao gồm enrich details và address
     */
    private OrderDTO buildDto(Orders o) {
        OrderDTO dto = orderMapper.toDTO(o);
        dto.setOrderDetails(
                enrichOrderDetails(orderDetailRepository.findByOrderId(o.getId()))
        );
        dto.setAddressDetails(
                addressRepository.findById(o.getAddressId())
                        .map(addressMapper::toDTO)
                        .orElse(null)
        );
        if (o.getPromotionId() != null) {
            promotionRepository.findById(o.getPromotionId())
                    .ifPresent(p -> dto.setPromotionCode(p.getCode()));
        }
        dto.setCreatedAt(o.getCreatedAt());
        return dto;
    }


    @Override
    @Transactional
    public Page<OrderDTO> getAllOrders(Long staffId,
                                       OrderStatus status,
                                       Long userId,
                                       LocalDateTime startDate,
                                       LocalDateTime endDate,
                                       Pageable pageable) {
        validateStaff(staffId);

        Page<Orders> ordersPage = orderRepository
                .findOrdersWithFilters(status, userId, startDate, endDate, pageable);

        List<OrderDTO> orderDTOs = ordersPage.getContent().stream()
                .map(order -> {
                    OrderDTO dto = orderMapper.toDTO(order);

                    // details + address + promotion như cũ...
                    dto.setOrderDetails(
                            enrichOrderDetails(
                                    orderDetailRepository.findByOrderId(dto.getId())
                            )
                    );
                    dto.setAddressDetails(
                            addressRepository.findById(order.getAddressId())
                                    .map(addressMapper::toDTO)
                                    .orElse(null)
                    );
                    if (order.getPromotionId() != null) {
                        promotionRepository.findById(order.getPromotionId())
                                .ifPresent(p -> dto.setPromotionCode(p.getCode()));
                    }

                    //lấy payment method
                    Payments payment = paymentRepository
                            .findFirstByOrderIdOrderByCreatedAtDesc(order.getId());
                    if (payment != null) {
                        dto.setPaymentMethod(payment.getPaymentMethod());
                    }

                    dto.setCreatedAt(order.getCreatedAt());
                    return dto;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(orderDTOs,
                pageable,
                ordersPage.getTotalElements());
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