package com.fail.app.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdminActionRequest(
        @NotBlank String reason
) {
}
