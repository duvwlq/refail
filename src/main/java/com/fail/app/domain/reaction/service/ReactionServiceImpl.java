package com.fail.app.domain.reaction.service;

import com.fail.app.common.exception.ApiException;
import com.fail.app.common.exception.ErrorCode;
import com.fail.app.domain.post.entity.Post;
import com.fail.app.domain.post.repository.PostRepository;
import com.fail.app.domain.reaction.dto.request.UpsertReactionRequest;
import com.fail.app.domain.reaction.dto.response.ReactionResponse;
import com.fail.app.domain.reaction.dto.response.ReactionCountResponse;
import com.fail.app.domain.reaction.entity.ReactionType;
import java.util.List;
import com.fail.app.domain.reaction.entity.Reaction;
import com.fail.app.domain.reaction.repository.ReactionRepository;
import com.fail.app.domain.user.entity.User;
import com.fail.app.domain.user.entity.UserStatus;
import com.fail.app.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReactionServiceImpl implements ReactionService {

    private final ReactionRepository reactionRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Override
    public ReactionResponse getReaction(Long userId, Long postId) {
        getActiveUser(userId);
        getPost(postId);
        return reactionRepository.findByPostIdAndUserId(postId, userId)
                .map(reaction -> new ReactionResponse(postId, reaction.getReactionType(), true))
                .orElseGet(() -> new ReactionResponse(postId, null, false));
    }

    @Override
    public List<ReactionCountResponse> getReactionCounts(Long postId) {
        getPost(postId);
        return reactionRepository.countByTypeForPost(postId).stream()
                .map(row -> new ReactionCountResponse((ReactionType) row[0], (Long) row[1]))
                .toList();
    }

    @Override
    @Transactional
    public ReactionResponse upsertReaction(Long userId, Long postId, UpsertReactionRequest request) {
        User user = getActiveUser(userId);
        Post post = getPost(postId);
        if (post.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        Reaction reaction = reactionRepository.findByPostIdAndUserId(postId, userId)
                .orElse(null);

        if (reaction == null) {
            reaction = reactionRepository.save(Reaction.builder()
                    .post(post)
                    .user(user)
                    .reactionType(request.reactionType())
                    .build());
            postRepository.incrementReactionCount(postId);
        } else {
            reaction.changeReactionType(request.reactionType());
        }

        return new ReactionResponse(postId, reaction.getReactionType(), true);
    }

    @Override
    @Transactional
    public ReactionResponse removeReaction(Long userId, Long postId) {
        getActiveUser(userId);
        Post post = getPost(postId);
        reactionRepository.findByPostIdAndUserId(postId, userId).ifPresent(reaction -> {
            reactionRepository.delete(reaction);
            postRepository.decrementReactionCount(postId);
        });
        return new ReactionResponse(postId, null, false);
    }

    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() == UserStatus.RESTRICTED) {
            throw new ApiException(ErrorCode.USER_RESTRICTED);
        }
        return user;
    }

    private Post getPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(ErrorCode.POST_NOT_FOUND));
        if (post.isDeleted() || post.isHidden()) {
            throw new ApiException(ErrorCode.POST_NOT_FOUND);
        }
        return post;
    }
}
