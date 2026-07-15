package com.fail.app.common.security;

import com.fail.app.domain.user.entity.UserRole;

public record CurrentUser(
        Long userId,
        String email,
        String nickname,
        UserRole role
) {
}
