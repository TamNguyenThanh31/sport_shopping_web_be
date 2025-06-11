package com.runner.shopping.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "strava")
@Data
public class StravaProperties {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String apiBaseUrl;
}
