package com.fail.app.domain.post.dto.request;

import com.fail.app.domain.post.entity.AdvicePreference;
import com.fail.app.domain.post.entity.FailureSize;
import com.fail.app.domain.post.entity.PostVisibilityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 100) String title,
        @NotBlank String content,
        @NotNull PostVisibilityType visibilityType,
        @NotNull FailureSize failureSize,
        @NotBlank @Size(max = 30) String emotionTag,
        @NotNull AdvicePreference advicePreference,
        @NotNull Boolean retryIntention,
        @Size(max = 500) String nextAttemptPlan
) {
}
