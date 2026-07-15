package com.fail.app.domain.reaction.service;

import com.fail.app.domain.reaction.dto.request.UpsertReactionRequest;
import com.fail.app.domain.reaction.dto.response.ReactionResponse;
import com.fail.app.domain.reaction.dto.response.ReactionCountResponse;
import java.util.List;

public interface ReactionService {

    ReactionResponse getReaction(Long userId, Long postId);

    List<ReactionCountResponse> getReactionCounts(Long postId);

    ReactionResponse upsertReaction(Long userId, Long postId, UpsertReactionRequest request);

    ReactionResponse removeReaction(Long userId, Long postId);
}
