package com.fail.app.domain.admin.service;

import com.fail.app.common.exception.ApiException;
import com.fail.app.common.exception.ErrorCode;
import com.fail.app.domain.post.entity.Post;
import com.fail.app.domain.post.repository.PostRepository;
import com.fail.app.domain.user.entity.User;
import com.fail.app.domain.user.entity.UserRole;
import com.fail.app.domain.user.policy.UserAccessPolicy;
import com.fail.app.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminAccessPolicy {

    private final UserAccessPolicy userAccessPolicy;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public User getAdmin(Long adminUserId) {
        User user = userAccessPolicy.getActiveUser(adminUserId);
        if (user.getRole() != UserRole.ADMIN) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        return user;
    }

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    public Post getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(ErrorCode.POST_NOT_FOUND));
    }
}
