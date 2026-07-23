package com.fail.app.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.observability")
public record ObservabilityProperties(
        boolean internalMetricsEnabled
) {
}
