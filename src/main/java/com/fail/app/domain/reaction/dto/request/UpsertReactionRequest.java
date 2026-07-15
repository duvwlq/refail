package com.fail.app.domain.reaction.dto.request;

import com.fail.app.domain.reaction.entity.ReactionType;
import jakarta.validation.constraints.NotNull;

public record UpsertReactionRequest(
        @NotNull ReactionType reactionType
) {
}
