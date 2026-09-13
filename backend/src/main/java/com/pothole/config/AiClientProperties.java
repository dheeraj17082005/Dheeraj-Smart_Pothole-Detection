package com.pothole.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties(prefix = "ai.service")
public record AiClientProperties(
        String url,
        Duration connectTimeout,
        Duration readTimeout
) {
    public AiClientProperties {
        if (url == null || url.isBlank()) {
            url = "http://localhost:8000";
        }
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(5);
        }
        if (readTimeout == null) {
            readTimeout = Duration.ofSeconds(15);
        }
    }
}
