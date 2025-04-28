package com.runner.shopping.controller;

import com.runner.shopping.entity.User;
import com.runner.shopping.enums.UserRole;
import com.runner.shopping.mapper.UserMapper;
import com.runner.shopping.model.dto.UserDTO;
import com.runner.shopping.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    // Quản lý CUSTOMER
    @GetMapping("/customers")
    public ResponseEntity<Page<UserDTO>> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Fetching customers: page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<UserDTO> customerPage = userService.findAllByRole(UserRole.CUSTOMER, pageable)
                .map(userMapper::toDTO);
        log.info("Retrieved {} customers (page {}, size {})", customerPage.getTotalElements(), page, size);
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
    public ResponseEntity<UserDTO> updateCustomer(@PathVariable Long id, @RequestBody User user) {
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

    // Quản lý STAFF
    @GetMapping("/staff")
    public ResponseEntity<Page<UserDTO>> getAllStaff(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Fetching staff: page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<UserDTO> staffPage = userService.findAllByRole(UserRole.STAFF, pageable)
                .map(userMapper::toDTO);
        log.info("Retrieved {} staff (page {}, size {})", staffPage.getTotalElements(), page, size);
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
    public ResponseEntity<UserDTO> updateStaff(@PathVariable Long id, @RequestBody User user) {
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
}
