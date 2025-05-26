package com.runner.shopping.util;

import com.runner.shopping.config.VnPayConfig;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
public class VnPayUtil {
    public static String hmacSHA512(String key, String data) {
        try {
            if (key == null || data == null) {
                throw new IllegalArgumentException("Key or data cannot be null");
            }
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] bytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * bytes.length);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Error creating HMAC SHA512", ex);
        }
    }

    public static String hashAllFields(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames); // Sắp xếp theo alphabet
        StringBuilder sb = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = fields.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                try {
                    // Mã hóa giá trị và thay %20 bằng +
                    String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString())
                            .replace("%20", "+");
                    sb.append(fieldName).append("=").append(encodedValue).append("&");
                } catch (Exception e) {
                    log.error("Error encoding field: name={}, value={}, error={}", fieldName, fieldValue, e.getMessage());
                    throw new RuntimeException("Error encoding VNPay parameters", e);
                }
            }
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        log.info("String to hash: {}", sb.toString());
        return VnPayUtil.hmacSHA512(VnPayConfig.VNP_HASH_SECRET, sb.toString());
    }
}