package com.fail.app.domain.reaction.dto.response;

import com.fail.app.domain.reaction.entity.ReactionType;

public record ReactionResponse(
        Long postId,
        ReactionType reactionType,
        boolean applied
) {
}
