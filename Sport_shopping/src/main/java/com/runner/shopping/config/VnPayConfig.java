package com.runner.shopping.config;


public class VnPayConfig {
    public static final String VNP_PAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static final String VNP_RETURN_URL = "http://localhost:8080/api/vnpay/return";
    public static final String VNP_TMN_CODE = "P23XJVVI";
    public static final String VNP_HASH_SECRET  = "YLOY0RV22I0BZ4PBUI0S3TRS3SY1Q0A2";
    public static final String VNP_API_URL = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
}
