package com.fail.app.common.health;

import java.time.LocalDateTime;

public record HealthResponse(
        String status,
        String application,
        String database,
        LocalDateTime checkedAt
) {
}
