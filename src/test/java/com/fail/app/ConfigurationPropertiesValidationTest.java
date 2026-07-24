package com.fail.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.fail.app.common.config.JwtProperties;
import com.fail.app.common.config.RateLimitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class ConfigurationPropertiesValidationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class)
            .withPropertyValues(
                    "jwt.secret=configuration-validation-secret-which-is-long-enough",
                    "jwt.access-token-validity-seconds=900",
                    "jwt.issuer=fail-test",
                    "jwt.refresh-token-validity-seconds=2592000",
                    "jwt.refresh-reuse-grace-seconds=5",
                    "jwt.refresh-cookie-secure=false",
                    "app.rate-limit.enabled=true",
                    "app.rate-limit.trust-forwarded-for=false",
                    "app.rate-limit.max-entries=10000",
                    "app.rate-limit.login.limit=5",
                    "app.rate-limit.login.window-seconds=60",
                    "app.rate-limit.signup.limit=3",
                    "app.rate-limit.signup.window-seconds=3600",
                    "app.rate-limit.refresh.limit=10",
                    "app.rate-limit.refresh.window-seconds=60",
                    "app.rate-limit.report.limit=5",
                    "app.rate-limit.report.window-seconds=3600"
            );

    @Test
    void rejectsShortJwtSecretDuringConfigurationBinding() {
        contextRunner
                .withPropertyValues("jwt.secret=too-short")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsNonPositiveRateLimitCapacityDuringConfigurationBinding() {
        contextRunner
                .withPropertyValues("app.rate-limit.max-entries=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({JwtProperties.class, RateLimitProperties.class})
    static class PropertiesConfiguration {
    }
}
