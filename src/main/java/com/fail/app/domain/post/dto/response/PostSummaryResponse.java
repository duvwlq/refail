package com.fail.app.domain.post.dto.response;

import com.fail.app.domain.post.entity.Post;
import com.fail.app.domain.post.entity.PostVisibilityType;
import java.time.LocalDateTime;

public record PostSummaryResponse(
        Long postId,
        Long categoryId,
        String categoryName,
        String title,
        String summary,
        PostVisibilityType visibilityType,
        String authorName,
        String failureSize,
        String emotionTag,
        int reactionCount,
        boolean hasUpdates,
        LocalDateTime createdAt
) {
    public static PostSummaryResponse of(Post post, boolean hasUpdates) {
        return new PostSummaryResponse(
                post.getId(),
                post.getCategory().getId(),
                post.getCategory().getName(),
                post.getTitle(),
                summarize(post.getContent()),
                post.getVisibilityType(),
                post.getVisibilityType() == PostVisibilityType.ANONYMOUS ? "익명" : post.getUser().getNickname(),
                post.getFailureSize().name(),
                post.getEmotionTag(),
                post.getReactionCount(),
                hasUpdates,
                post.getCreatedAt()
        );
    }

    private static String summarize(String content) {
        return content.length() <= 80 ? content : content.substring(0, 80) + "...";
    }
}
