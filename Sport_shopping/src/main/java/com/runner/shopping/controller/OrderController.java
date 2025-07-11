package com.runner.shopping.controller;

import com.runner.shopping.enums.OrderStatus;
import com.runner.shopping.model.dto.OrderDTO;
import com.runner.shopping.service.OrderService;
import com.runner.shopping.service.VnPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final VnPayService vnPayService;

    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@Valid @RequestBody OrderDTO orderDTO) {
        OrderDTO createdOrder = orderService.createOrder(orderDTO);
        return ResponseEntity.ok(createdOrder);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id, @RequestParam Long userId) {
        OrderDTO orderDTO = orderService.getOrderById(id, userId);
        return ResponseEntity.ok(orderDTO);
    }

    @GetMapping
    public ResponseEntity<List<OrderDTO>> getOrdersByUserId(@RequestParam Long userId) {
        List<OrderDTO> orders = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id, @RequestParam Long userId) {
        log.info("Cancelling order ID: {} for userId: {}", id, userId);
        orderService.cancelOrder(id, userId);
        log.info("Cancelled order ID: {}", id);
        return ResponseEntity.noContent().build();
    }

//    @PutMapping("/{id}/status")
//    public ResponseEntity<OrderDTO> updateOrderStatus(@PathVariable Long id,
//                                                      @RequestParam OrderStatus status,
//                                                      @RequestParam Long staffId) {
//        log.info("Updating status of order ID: {} to {} by staffId: {}", id, status, staffId);
//        OrderDTO updatedOrder = orderService.updateOrderStatus(id, status, staffId);
//        log.info("Updated order ID: {} to status: {}", id, status);
//        return ResponseEntity.ok(updatedOrder);
//    }

    @GetMapping("/all")
    @Operation(summary = "Get all orders with filters and pagination", description = "Retrieves all orders with optional filters and pagination for staff or admin")
    public ResponseEntity<Page<OrderDTO>> getAllOrders(
            @Parameter(description = "Staff ID who makes the request") @RequestParam Long staffId,
            @Parameter(description = "Filter by order status (e.g., PENDING, PROCESSING)") @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "Filter by user ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "Filter by start date (yyyy-MM-dd'T'HH:mm:ss)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Filter by end date (yyyy-MM-dd'T'HH:mm:ss)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "20") int size) {
        log.info("Fetching all orders for staffId: {}, status: {}, userId: {}, startDate: {}, endDate: {}, page: {}, size: {}",
                staffId, status, userId, startDate, endDate, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderDTO> orders = orderService.getAllOrders(staffId, status, userId, startDate, endDate, pageable);
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/{id}/vnpay")
    @Operation(summary = "Initiate VNPay payment", description = "Generates a VNPay payment URL for an order")
    public ResponseEntity<Map<String, String>> initiateVNPayPayment(
            @Parameter(description = "Order ID") @PathVariable Long id,
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Return URL for VNPay callback") @RequestParam String returnUrl) {
        log.info("Initiating VNPay payment for order ID: {} by userId: {}", id, userId);
        String vnpayUrl = orderService.initiateVNPayPayment(id, userId, returnUrl);
        log.info("Generated VNPay URL for order ID: {}", id);
        return ResponseEntity.ok(Map.of("paymentUrl", vnpayUrl));
    }
}