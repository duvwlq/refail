package com.fail.app.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fail.app.common.config.RateLimitProperties;
import com.fail.app.common.exception.ErrorCode;
import com.fail.app.common.response.ErrorResponse;
import com.fail.app.common.security.CurrentUser;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final InMemoryRateLimiter rateLimiter;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(
            RateLimitProperties properties,
            InMemoryRateLimiter rateLimiter,
            MeterRegistry meterRegistry,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Route route = resolveRoute(request);
        if (!properties.enabled() || route == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String identity = route.userBased() ? currentUserId() : clientAddress(request);
        if (identity == null) {
            filterChain.doFilter(request, response);
            return;
        }

        var decision = rateLimiter.tryAcquire(route.name() + ":" + identity, route.policy());
        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        meterRegistry.counter("refail.rate_limit.exceeded", "endpoint", route.name()).increment();
        response.setStatus(ErrorCode.RATE_LIMIT_EXCEEDED.getStatus().value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), ErrorResponse.builder()
                .code(ErrorCode.RATE_LIMIT_EXCEEDED.getCode())
                .message(ErrorCode.RATE_LIMIT_EXCEEDED.getMessage())
                .timestamp(LocalDateTime.now())
                .build());
    }

    private Route resolveRoute(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) {
            return null;
        }
        String path = request.getRequestURI();
        return switch (path) {
            case "/api/v1/auth/login" -> new Route("login", properties.login(), false);
            case "/api/v1/auth/signup" -> new Route("signup", properties.signup(), false);
            case "/api/v1/auth/refresh" -> new Route("refresh", properties.refresh(), false);
            default -> path.matches("/api/v1/posts/\\d+/reports")
                    ? new Route("report", properties.report(), true)
                    : null;
        };
    }

    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
            return null;
        }
        return String.valueOf(currentUser.userId());
    }

    private String clientAddress(HttpServletRequest request) {
        if (properties.trustForwardedFor()) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null) {
                String firstAddress = forwardedFor.split(",", 2)[0].trim();
                if (firstAddress.matches("[0-9a-fA-F:.]{1,45}")) {
                    return firstAddress;
                }
            }
        }
        return request.getRemoteAddr();
    }

    private record Route(String name, RateLimitProperties.Limit policy, boolean userBased) {
    }
}
