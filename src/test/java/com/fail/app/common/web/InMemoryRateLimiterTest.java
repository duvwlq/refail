package com.fail.app.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.fail.app.common.config.RateLimitProperties;
import org.junit.jupiter.api.Test;

class InMemoryRateLimiterTest {

    @Test
    void keySpaceNeverGrowsBeyondConfiguredCapacity() {
        var policy = new RateLimitProperties.Limit(10, 60);
        var properties = new RateLimitProperties(
                true,
                false,
                2,
                policy,
                policy,
                policy,
                policy
        );
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(properties);

        assertThat(limiter.tryAcquire("login:one", policy).allowed()).isTrue();
        assertThat(limiter.tryAcquire("login:two", policy).allowed()).isTrue();
        assertThat(limiter.tryAcquire("login:three", policy).allowed()).isTrue();

        assertThat(limiter.size()).isEqualTo(2);
    }
}
