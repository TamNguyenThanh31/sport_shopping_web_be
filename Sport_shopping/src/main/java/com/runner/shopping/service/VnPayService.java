package com.runner.shopping.service;

import jakarta.servlet.http.HttpServletRequest;

public interface VnPayService {
    String handleVnpayReturn(HttpServletRequest request);
}
