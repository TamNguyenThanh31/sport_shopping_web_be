package com.runner.shopping.service;

import com.runner.shopping.model.dto.PromotionDTO;
import com.runner.shopping.model.dto.StravaStatusDTO;

public interface StravaService {
    /** Lưu token lần đầu hoặc refresh, update stravaId lên User */
    void connect(Long userId, String code);

    /** Lấy status với number of days (ví dụ 7 hoặc 30) */
    StravaStatusDTO getStatus(Long userId, int days);

    /** Mặc định lấy status 7 ngày */
    default StravaStatusDTO getStatus(Long userId) {
        return getStatus(userId, 7);
    }

    /** Tạo coupon dựa trên availableKm và trả về PromotionDTO */
    PromotionDTO redeemCoupon(Long userId, int metres);
}
