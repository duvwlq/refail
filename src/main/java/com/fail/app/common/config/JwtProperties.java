package com.fail.app.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTokenValiditySeconds,
        String issuer,
        long refreshTokenValiditySeconds,
        long refreshReuseGraceSeconds,
        boolean refreshCookieSecure
) {
}
