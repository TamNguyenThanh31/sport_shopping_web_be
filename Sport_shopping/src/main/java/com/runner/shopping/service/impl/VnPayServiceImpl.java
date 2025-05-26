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
    public String handleVnpayReturn(HttpServletRequest request) {
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
                throw new RuntimeException("Error decoding VNPay parameters", e);
            }
        }

        log.info("VNPay Callback Parameters after decoding: {}", fields);

        String secureHash = fields.get("vnp_SecureHash");
        if (secureHash == null) {
            log.warn("vnp_SecureHash is missing in callback parameters");
            return "redirect:/payment-result?status=failed&message=Missing+secure+hash";
        }

        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        String orderIdStr = fields.get("vnp_TxnRef");
        if (orderIdStr == null || orderIdStr.isEmpty()) {
            log.warn("vnp_TxnRef is missing in callback parameters");
            return "redirect:/payment-result?status=failed&message=Missing+transaction+reference";
        }

        // Tách orderId từ vnp_TxnRef (dạng <orderId>_<timestamp>)
        String orderId;
        try {
            orderId = orderIdStr.split("_")[0]; // Lấy phần orderId trước dấu _
        } catch (Exception e) {
            log.error("Invalid vnp_TxnRef format: {}", orderIdStr);
            return "redirect:/payment-result?status=failed&message=Invalid+transaction+reference+format";
        }

        // Chuyển orderId thành Long
        Long orderIdLong;
        try {
            orderIdLong = Long.parseLong(orderId);
        } catch (NumberFormatException e) {
            log.error("Invalid orderId in vnp_TxnRef: {}", orderId);
            return "redirect:/payment-result?status=failed&message=Invalid+order+id";
        }

        Payments payment = paymentRepository.findByOrderId(orderIdLong).orElse(null);
        if (payment == null) {
            log.warn("Payment not found for orderId={}", orderIdLong);
            return "redirect:/payment-result?status=failed&message=Payment+not+found";
        }

        String signValue = VnPayUtil.hashAllFields(fields);
        log.info("Calculated signValue: {}, Received secureHash: {}", signValue, secureHash);
        if (!secureHash.equals(signValue)) {
            log.warn("Invalid VNPay signature: expected={}, actual={}", signValue, secureHash);
            return "redirect:/payment-result?status=failed&message=Invalid+signature";
        }

        String transactionStatus = fields.get("vnp_TransactionStatus");
        String responseCode = fields.get("vnp_ResponseCode");

        Orders order = orderRepository.findById(orderIdLong).orElse(null);
        if (order == null) {
            log.warn("Order not found for orderId={}", orderIdLong);
            return "redirect:/payment-result?status=failed&message=Order+not+found";
        }

        if (!order.getStatus().equals(OrderStatus.PENDING) && !order.getStatus().equals(OrderStatus.CONFIRMED)) {
            log.warn("Order already processed: orderId={}, status={}", orderIdLong, order.getStatus());
            return "redirect:/payment-result?status=failed&message=Order+already+processed";
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

        return "redirect:/payment-result?status=" + (isSuccess ? "success" : "failed");
    }

    // Thêm phương thức để hoàn stock
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
                log.setCreatedBy(null); // Có thể set userId nếu có
                inventoryLogRepository.save(log);
            }
        }
    }
}
