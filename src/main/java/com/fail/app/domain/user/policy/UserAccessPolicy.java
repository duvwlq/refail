package com.fail.app.domain.user.policy;

import com.fail.app.common.exception.ApiException;
import com.fail.app.common.exception.ErrorCode;
import com.fail.app.domain.user.entity.User;
import com.fail.app.domain.user.entity.UserStatus;
import com.fail.app.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAccessPolicy {

    private final UserRepository userRepository;

    public User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        validateActive(user);
        return user;
    }

    public void validateActive(User user) {
        if (user.getStatus() == UserStatus.DELETED || user.isDeleted()) {
            throw new ApiException(ErrorCode.USER_DELETED);
        }
        if (user.getStatus() == UserStatus.RESTRICTED) {
            throw new ApiException(ErrorCode.USER_RESTRICTED);
        }
    }
}
