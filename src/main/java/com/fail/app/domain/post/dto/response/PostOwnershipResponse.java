package com.fail.app.domain.post.dto.response;

public record PostOwnershipResponse(
        Long postId,
        boolean ownedByMe
) {
}
