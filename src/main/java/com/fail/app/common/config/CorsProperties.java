package com.fail.app.common.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        List<String> allowedOrigins
) {
    public List<String> resolvedAllowedOrigins() {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            return List.of("http://localhost:3000");
        }
        return allowedOrigins;
    }
}
