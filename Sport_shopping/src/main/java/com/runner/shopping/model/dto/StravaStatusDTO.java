package com.runner.shopping.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
public class StravaStatusDTO {
    private double totalDistanceKm;   // tổng km 7 ngày
    private String averagePace;       // format "mm:ss"
    private int AvailableKm;          // floor(total) - usedKm
    private int currentDiscount;      // (availableKm/5)*10
    /** true nếu đã hoặc sắp dùng hết hạn mức tháng */
    private boolean nearLimit;

    /** Thông điệp cảnh báo khi nearLimit == true */
    private String warningMessage;
}
