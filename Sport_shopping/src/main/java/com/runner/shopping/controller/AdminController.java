package com.runner.shopping.controller;

import com.runner.shopping.entity.User;
import com.runner.shopping.enums.OrderStatus;
import com.runner.shopping.enums.UserRole;
import com.runner.shopping.mapper.UserMapper;
import com.runner.shopping.model.dto.*;
import com.runner.shopping.service.OrderService;
import com.runner.shopping.service.ReportService;
import com.runner.shopping.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private final ReportService reportService;

    private final OrderService orderService;

    // ──────────────────────────────────────────────────────────────────────────────
    // Quản lý CUSTOMER
    // ──────────────────────────────────────────────────────────────────────────────

    @GetMapping("/customers")
    public ResponseEntity<Page<UserDTO>> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Fetching customers: page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<UserDTO> customerPage = userService
                .findAllByRole(UserRole.CUSTOMER, pageable)
                .map(userMapper::toDTO);
        log.info("Retrieved {} customers (page {}, size {})",
                customerPage.getTotalElements(), page, size);
        return ResponseEntity.ok(customerPage);
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<UserDTO> getCustomerById(@PathVariable Long id) {
        User customer = userService.findById(id);
        if (customer.getRole() != UserRole.CUSTOMER) {
            throw new IllegalArgumentException("User is not a customer");
        }
        return ResponseEntity.ok(userMapper.toDTO(customer));
    }

    @PostMapping("/customers")
    public ResponseEntity<UserDTO> createCustomer(@RequestBody User user) {
        user.setRole(UserRole.CUSTOMER);
        User createdUser = userService.registerUser(user);
        return ResponseEntity.ok(userMapper.toDTO(createdUser));
    }

    @PutMapping("/customers/{id}")
    public ResponseEntity<UserDTO> updateCustomer(
            @PathVariable Long id,
            @RequestBody User user) {
        User existingUser = userService.findById(id);
        if (existingUser.getRole() != UserRole.CUSTOMER) {
            throw new IllegalArgumentException("User is not a customer");
        }
        existingUser.setUsername(user.getUsername());
        existingUser.setEmail(user.getEmail());
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            existingUser.setPassword(user.getPassword());
        }
        User updatedUser = userService.updateUser(existingUser);
        return ResponseEntity.ok(userMapper.toDTO(updatedUser));
    }

    @DeleteMapping("/customers/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user.getRole() != UserRole.CUSTOMER) {
            throw new IllegalArgumentException("User is not a customer");
        }
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Quản lý STAFF
    // ──────────────────────────────────────────────────────────────────────────────

    @GetMapping("/staff")
    public ResponseEntity<Page<UserDTO>> getAllStaff(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Fetching staff: page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<UserDTO> staffPage = userService
                .findAllByRole(UserRole.STAFF, pageable)
                .map(userMapper::toDTO);
        log.info("Retrieved {} staff (page {}, size {})",
                staffPage.getTotalElements(), page, size);
        return ResponseEntity.ok(staffPage);
    }

    @GetMapping("/staff/{id}")
    public ResponseEntity<UserDTO> getStaffById(@PathVariable Long id) {
        User staff = userService.findById(id);
        if (staff.getRole() != UserRole.STAFF) {
            throw new IllegalArgumentException("User is not a staff member");
        }
        return ResponseEntity.ok(userMapper.toDTO(staff));
    }

    @PostMapping("/staff")
    public ResponseEntity<UserDTO> createStaff(@RequestBody User user) {
        user.setRole(UserRole.STAFF);
        User createdUser = userService.registerUser(user);
        return ResponseEntity.ok(userMapper.toDTO(createdUser));
    }

    @PutMapping("/staff/{id}")
    public ResponseEntity<UserDTO> updateStaff(
            @PathVariable Long id,
            @RequestBody User user) {
        User existingUser = userService.findById(id);
        if (existingUser.getRole() != UserRole.STAFF) {
            throw new IllegalArgumentException("User is not a staff member");
        }
        existingUser.setUsername(user.getUsername());
        existingUser.setEmail(user.getEmail());
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            existingUser.setPassword(user.getPassword());
        }
        User updatedUser = userService.updateUser(existingUser);
        return ResponseEntity.ok(userMapper.toDTO(updatedUser));
    }

    @DeleteMapping("/staff/{id}")
    public ResponseEntity<Void> deleteStaff(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user.getRole() != UserRole.STAFF) {
            throw new IllegalArgumentException("User is not a staff member");
        }
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // API CHO DASHBOARD – “HÔM NAY”
    // ──────────────────────────────────────────────────────────────────────────────

    /** GET /api/admin/reports/orders/today */
    @GetMapping("/reports/orders/today")
    public ResponseEntity<Map<String, Long>> getTotalOrdersToday() {
        Long totalOrders = reportService.countOrdersToday();
        return ResponseEntity.ok(Map.of("totalOrdersToday", totalOrders));
    }

    /** GET /api/admin/reports/revenue/today */
    @GetMapping("/reports/revenue/today")
    public ResponseEntity<Map<String, BigDecimal>> getRevenueToday() {
        BigDecimal revenue = reportService.sumRevenueToday();
        return ResponseEntity.ok(Map.of("revenueToday", revenue));
    }

    /** GET /api/admin/reports/profit/today */
    @GetMapping("/reports/profit/today")
    public ResponseEntity<Map<String, BigDecimal>> getProfitToday() {
        BigDecimal profit = reportService.sumProfitToday();
        return ResponseEntity.ok(Map.of("profitToday", profit));
    }

    // ────────────────────────────────────────────────────────────────
    // API CHO DASHBOARD – KHOẢNG THỜI GIAN TUỲ CHỌN (THAY CHO TUẦN, THÁNG)
    // ────────────────────────────────────────────────────────────────

    /**
     * GET /api/admin/reports/revenue
     * Tham số query: startDate, endDate (ISO 8601), bắt buộc startDate, endDate tùy chọn
     * Ví dụ: /api/admin/reports/revenue?startDate=2025-06-01T00:00:00&endDate=2025-06-30T23:59:59
     */
    @GetMapping("/reports/revenue")
    public ResponseEntity<Map<String, BigDecimal>> getRevenueBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        BigDecimal revenue = reportService.sumRevenueBetween(startDate, endDate);
        return ResponseEntity.ok(Map.of("revenue", revenue));
    }

    /**
     * GET /api/admin/reports/profit
     * Tham số query: startDate, endDate (ISO 8601)
     */
    @GetMapping("/reports/profit")
    public ResponseEntity<Map<String, BigDecimal>> getProfitBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        BigDecimal profit = reportService.sumProfitBetween(startDate, endDate);
        return ResponseEntity.ok(Map.of("profit", profit));
    }

    // ────────────────────────────────────────────────────────────────
    // API CHI TIẾT DOANH THU LỢI NHUẬN THEO KHOẢNG THỜI GIAN TUỲ CHỌN
    // ────────────────────────────────────────────────────────────────

    /**
     * GET /api/admin/reports/revenue/detail
     * Tham số: staffId, startDate, endDate, pageable
     * Ví dụ: /api/admin/reports/revenue/detail?staffId=123&startDate=2025-06-01T00:00:00&endDate=2025-06-30T23:59:59&page=0&size=10
     */
    @GetMapping("/reports/revenue/detail")
    public Page<ReportOrderDTO> detailByDateRange(
            @RequestParam Long staffId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Pageable pageable
    ) {
        return reportService.revenueDetailByDateRange(staffId, startDate, endDate, pageable);
    }

    // ────────────────────────────────────────────────────────────────
        //API CHI TIẾT DOANH THU LỢI NHUẬN TRONG NGÀY HÔM NAY
    // ────────────────────────────────────────────────────────────────

    @GetMapping("/reports/revenue/detail/today")
    public Page<ReportOrderDTO> detailToday(
            @RequestParam Long staffId,
            Pageable pageable
    ) {
        return reportService.revenueDetailToday(staffId, pageable);
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // API CHO DASHBOARD – TỒN KHO THEO TÊN SẢN PHẨM
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/admin/reports/stock/by-name
     * Trả về Map<productName, List<ProductVariantInfoDTO>>
     */
    @GetMapping("/reports/stock/by-name")
    public ResponseEntity<Map<String, List<ProductVariantInfoDTO>>> getStockByName() {
        Map<String, List<ProductVariantInfoDTO>> data = reportService.getCurrentStockByName();
        return ResponseEntity.ok(data);
    }


    // ──────────────────────────────────────────────────────────────────────────────
        // API CHO DASHBOARD – TOP SẢN PHẨM BÁN CHẠY
    // ──────────────────────────────────────────────────────────────────────────────
    @GetMapping("/reports/top-selling")
    public ResponseEntity<List<TopSellingProductDTO>> getTopSellingProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false, defaultValue = "10") int limit
    ) {
        List<TopSellingProductDTO> topProducts = orderService.getTopSellingProducts(startDate, endDate, limit);
        return ResponseEntity.ok(topProducts);
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // API CHO ORDER
    // ──────────────────────────────────────────────────────────────────────────────

    @PutMapping("/{id}/status-order")
    public ResponseEntity<OrderDTO> updateOrderStatus(@PathVariable Long id,
                                                      @RequestParam OrderStatus status,
                                                      @RequestParam Long staffId) {
        log.info("Updating status of order ID: {} to {} by staffId: {}", id, status, staffId);
        OrderDTO updatedOrder = orderService.updateOrderStatus(id, status, staffId);
        log.info("Updated order ID: {} to status: {}", id, status);
        return ResponseEntity.ok(updatedOrder);
    }

    @GetMapping("/all-order")
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
}
