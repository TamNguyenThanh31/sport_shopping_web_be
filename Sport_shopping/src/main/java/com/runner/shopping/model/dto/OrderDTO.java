package com.runner.shopping.model.dto;

import com.runner.shopping.enums.OrderStatus;
import com.runner.shopping.enums.PaymentMethod;
import com.runner.shopping.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDTO {

    @Schema(description = "Order ID")
    private Long id;

    @Schema(description = "User ID who placed the order")
    @NotNull(message = "User ID is required")
    private Long userId;

    @Schema(description = "Total price of the order")
    private BigDecimal totalPrice;

    @Schema(description = "Order status")
    private OrderStatus status;

    @Schema(description = "Payment status")
    private PaymentStatus paymentStatus;

    @Schema(description = "Staff ID who handled the order")
    private Long handledBy;

    @Schema(description = "Address ID for shipping")
    @NotNull(message = "Address ID is required")
    private Long addressId;

    @Schema(description = "Details of the shipping address")
    private AddressDTO addressDetails;

    @Schema(description = "Promotion ID applied to the order")
    private Long promotionId;

    @Schema(description = "Promotion code applied to the order")
    private String promotionCode;

    @Schema(description = "Payment method")
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @Schema(description = "Order creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "List of order details, populated in response (not required in request)")
    private List<OrderDetailDTO> orderDetails;
}