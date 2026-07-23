package com.fail.app.domain.auth.service;

import com.fail.app.domain.auth.dto.response.LoginResponse;
import java.time.Duration;

public record IssuedAuthSession(
        LoginResponse response,
        String refreshToken,
        Duration refreshTokenMaxAge
) {
}
