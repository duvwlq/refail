package com.fail.app.common.response;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ErrorResponse(
        String code,
        String message,
        LocalDateTime timestamp
) {
}
