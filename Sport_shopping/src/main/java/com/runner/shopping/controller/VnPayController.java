package com.runner.shopping.controller;

import com.runner.shopping.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vnpay")
@RequiredArgsConstructor

public class VnPayController {

    private final VnPayService paymentService;

    @GetMapping("/return")
    public ResponseEntity<Void> vnpayReturn(HttpServletRequest request) {
        return paymentService.handleVnpayReturn(request);
    }
}
