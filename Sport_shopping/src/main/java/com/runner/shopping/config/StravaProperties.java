package com.runner.shopping.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "strava")
@Data
public class StravaProperties {
    // OAuth
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String apiBaseUrl;

    // Giới hạn quãng đường
    private int maxDailyMeters;
    private int maxMonthlyMeters;

    // Lọc pace
    private int minPaceSecPerKm;
    private int maxPaceSecPerKm;

    // Giới hạn redeem
    private int maxRedeemsPerMonth;

    // Cảnh báo sắp chạm ngưỡng
    private int warnThresholdPercent;
}
