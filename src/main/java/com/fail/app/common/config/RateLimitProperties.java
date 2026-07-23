package com.fail.app.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        boolean trustForwardedFor,
        int maxEntries,
        Limit login,
        Limit signup,
        Limit refresh,
        Limit report
) {

    public record Limit(int limit, long windowSeconds) {
    }
}
