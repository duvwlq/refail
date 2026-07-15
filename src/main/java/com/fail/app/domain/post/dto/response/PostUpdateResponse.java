package com.fail.app.domain.post.dto.response;

import com.fail.app.domain.post.entity.PostUpdate;
import java.time.LocalDateTime;

public record PostUpdateResponse(
        Long updateId,
        String status,
        String content,
        LocalDateTime createdAt
) {
    public static PostUpdateResponse from(PostUpdate postUpdate) {
        return new PostUpdateResponse(
                postUpdate.getId(),
                postUpdate.getStatus().name(),
                postUpdate.getContent(),
                postUpdate.getCreatedAt()
        );
    }
}
