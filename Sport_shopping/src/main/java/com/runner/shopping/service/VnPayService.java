package com.runner.shopping.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface VnPayService {
    ResponseEntity<Void> handleVnpayReturn(HttpServletRequest request);
}
