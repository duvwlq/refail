package com.fail.app.domain.admin.dto.response;

import java.time.LocalDateTime;

public record AdminActionResponse(
        Long targetId,
        boolean hidden,
        String userStatus,
        LocalDateTime processedAt
) {
}
