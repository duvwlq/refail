package com.fail.app.common.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "jwt")
@Validated
public record JwtProperties(
        @NotBlank @Size(min = 32) String secret,
        @Positive
        long accessTokenValiditySeconds,
        @NotBlank
        String issuer,
        @Positive
        long refreshTokenValiditySeconds,
        @PositiveOrZero
        long refreshReuseGraceSeconds,
        boolean refreshCookieSecure
) {
}
