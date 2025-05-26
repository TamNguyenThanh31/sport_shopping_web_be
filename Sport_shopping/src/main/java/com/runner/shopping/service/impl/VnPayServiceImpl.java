package com.runner.shopping.service.impl;

import com.runner.shopping.entity.*;
import com.runner.shopping.enums.OrderStatus;
import com.runner.shopping.enums.PaymentStatus;
import com.runner.shopping.repository.*;
import com.runner.shopping.service.VnPayService;
import com.runner.shopping.util.VnPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VnPayServiceImpl implements VnPayService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final InventoryLogRepository inventoryLogRepository;

    @Override
    @Transactional
    public ResponseEntity<Void> handleVnpayReturn(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            String value = request.getParameter(name);
            try {
                String decodedName = URLDecoder.decode(name, StandardCharsets.UTF_8.toString());
                String decodedValue = value != null ? URLDecoder.decode(value, StandardCharsets.UTF_8.toString()) : "";
                fields.put(decodedName, decodedValue);
            } catch (Exception e) {
                log.error("Error decoding VNPay parameter: name={}, value={}, error={}", name, value, e.getMessage());
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header("Location", "http://localhost:4200/payment-result?status=failed&message=Error+decoding+parameters")
                        .build();
            }
        }

        log.info("VNPay Callback Parameters after decoding: {}", fields);

        String secureHash = fields.get("vnp_SecureHash");
        if (secureHash == null) {
            log.warn("vnp_SecureHash is missing in callback parameters");
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "http://localhost:4200/payment-result?status=failed&message=Missing+secure+hash")
                    .build();
        }

        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        String orderIdStr = fields.get("vnp_TxnRef");
        if (orderIdStr == null || orderIdStr.isEmpty()) {
            log.warn("vnp_TxnRef is missing in callback parameters");
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "http://localhost:4200/payment-result?status=failed&message=Missing+transaction+reference")
                    .build();
        }

        // Tách orderId từ vnp_TxnRef (dạng <orderId>_<timestamp>)
        String orderId;
        try {
            orderId = orderIdStr.split("_")[0];
        } catch (Exception e) {
            log.error("Invalid vnp_TxnRef format: {}", orderIdStr);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "http://localhost:4200/payment-result?status=failed&message=Invalid+transaction+reference+format")
                    .build();
        }

        // Chuyển orderId thành Long
        Long orderIdLong;
        try {
            orderIdLong = Long.parseLong(orderId);
        } catch (NumberFormatException e) {
            log.error("Invalid orderId in vnp_TxnRef: {}", orderId);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "http://localhost:4200/payment-result?status=failed&message=Invalid+order+id")
                    .build();
        }

        Payments payment = paymentRepository.findByOrderId(orderIdLong).orElse(null);
        if (payment == null) {
            log.warn("Payment not found for orderId={}", orderIdLong);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "http://localhost:4200/payment-result?status=failed&message=Payment+not+found")
                    .build();
        }

        String signValue = VnPayUtil.hashAllFields(fields);
        log.info("Calculated signValue: {}, Received secureHash: {}", signValue, secureHash);
        if (!secureHash.equals(signValue)) {
            log.warn("Invalid VNPay signature: expected={}, actual={}", signValue, secureHash);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "http://localhost:4200/payment-result?status=failed&message=Invalid+signature")
                    .build();
        }

        String transactionStatus = fields.get("vnp_TransactionStatus");
        String responseCode = fields.get("vnp_ResponseCode");

        Orders order = orderRepository.findById(orderIdLong).orElse(null);
        if (order == null) {
            log.warn("Order not found for orderId={}", orderIdLong);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "http://localhost:4200/payment-result?status=failed&message=Order+not+found")
                    .build();
        }

        if (!order.getStatus().equals(OrderStatus.PENDING) && !order.getStatus().equals(OrderStatus.CONFIRMED)) {
            log.warn("Order already processed: orderId={}, status={}", orderIdLong, order.getStatus());
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", "http://localhost:4200/payment-result?status=failed&message=Order+already+processed")
                    .build();
        }

        boolean isSuccess = "00".equals(transactionStatus) && "00".equals(responseCode);
        if (isSuccess) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setPaymentStatus(PaymentStatus.COMPLETED);
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setTransactionId(fields.get("vnp_TransactionNo"));
            log.info("Payment successful for orderId={}", orderIdLong);
        } else {
            order.setStatus(OrderStatus.CANCELLED);
            order.setPaymentStatus(PaymentStatus.FAILED);
            payment.setStatus(PaymentStatus.FAILED);
            log.info("Payment failed for orderId={}, responseCode={}, transactionStatus={}",
                    orderIdLong, responseCode, transactionStatus);
            revertStockForOrder(order);
        }
        orderRepository.save(order);
        paymentRepository.save(payment);

        // Redirect đến URL frontend
        String redirectUrl = "http://localhost:4200/payment-result?status=" + (isSuccess ? "success" : "failed") + "&orderId=" + orderId;
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", redirectUrl)
                .build();
    }

    private void revertStockForOrder(Orders order) {
        List<OrderDetails> details = orderDetailRepository.findByOrderId(order.getId());
        for (OrderDetails detail : details) {
            ProductVariant variant = productVariantRepository.findByIdNotDeletedWithLock(detail.getVariantId())
                    .orElse(null);
            if (variant != null) {
                variant.setStock(variant.getStock() + detail.getQuantity());
                productVariantRepository.save(variant);
                InventoryLogs log = new InventoryLogs();
                log.setVariantId(detail.getVariantId());
                log.setQuantityChange(detail.getQuantity());
                log.setReason("Hủy đơn hàng do thanh toán thất bại, ID: " + order.getId());
                log.setCreatedBy(null);
                inventoryLogRepository.save(log);
            }
        }
    }
}
