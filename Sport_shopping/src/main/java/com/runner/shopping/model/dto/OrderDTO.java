package com.runner.shopping.model.dto;

import com.runner.shopping.enums.OrderStatus;
import com.runner.shopping.enums.PaymentMethod;
import com.runner.shopping.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderDTO {

    private Long id;

    @NotNull(message = "User ID is required")
    private Long userId;

    private BigDecimal totalPrice;

    private OrderStatus status;

    private PaymentStatus paymentStatus;

    private Long handledBy;

    @NotNull(message = "Address ID is required")
    private Long addressId;

    private Long promotionId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @Schema(description = "List of order details, populated in response (not required in request)")
    private List<OrderDetailDTO> orderDetails;
}
