package com.fail.app.domain.post.dto.response;

import com.fail.app.domain.post.entity.Post;
import com.fail.app.domain.post.entity.PostVisibilityType;
import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResponse(
        Long postId,
        Long categoryId,
        String categoryName,
        String title,
        String content,
        PostVisibilityType visibilityType,
        String authorName,
        String failureSize,
        String emotionTag,
        String advicePreference,
        boolean retryIntention,
        String nextAttemptPlan,
        int reactionCount,
        List<PostUpdateResponse> updates,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostDetailResponse of(Post post, List<PostUpdateResponse> updates) {
        return new PostDetailResponse(
                post.getId(),
                post.getCategory().getId(),
                post.getCategory().getName(),
                post.getTitle(),
                post.getContent(),
                post.getVisibilityType(),
                post.getVisibilityType() == PostVisibilityType.ANONYMOUS ? "익명" : post.getUser().getNickname(),
                post.getFailureSize().name(),
                post.getEmotionTag(),
                post.getAdvicePreference().name(),
                post.isRetryIntention(),
                post.getNextAttemptPlan(),
                post.getReactionCount(),
                updates,
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
