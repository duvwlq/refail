package com.fail.app.domain.auth.dto.response;

import com.fail.app.domain.user.entity.User;
import com.fail.app.domain.user.entity.UserRole;
import com.fail.app.domain.user.entity.UserStatus;
import java.time.LocalDateTime;

public record AuthUserResponse(
        Long userId,
        String email,
        String nickname,
        UserRole role,
        UserStatus status,
        LocalDateTime createdAt
) {
    public static AuthUserResponse from(User user) {
        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
