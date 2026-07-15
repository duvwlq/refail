package com.fail.app.domain.post.dto.response;

public record DeletePostResponse(
        Long postId,
        boolean deleted
) {
}
