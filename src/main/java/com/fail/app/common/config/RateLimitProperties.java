package com.fail.app.common.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.rate-limit")
@Validated
public record RateLimitProperties(
        boolean enabled,
        boolean trustForwardedFor,
        @Positive
        int maxEntries,
        @Valid @NotNull Limit login,
        @Valid @NotNull Limit signup,
        @Valid @NotNull Limit refresh,
        @Valid @NotNull Limit report
) {

    public record Limit(
            @Positive int limit,
            @Positive long windowSeconds
    ) {
    }
}
