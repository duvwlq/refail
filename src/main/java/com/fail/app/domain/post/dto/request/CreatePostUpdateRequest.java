package com.fail.app.domain.post.dto.request;

import com.fail.app.domain.post.entity.PostUpdateStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePostUpdateRequest(
        @NotNull PostUpdateStatus status,
        @NotBlank String content
) {
}
