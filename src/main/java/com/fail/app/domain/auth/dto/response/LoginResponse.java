package com.fail.app.domain.auth.dto.response;

public record LoginResponse(
        String accessToken,
        String tokenType,
        AuthUserResponse user
) {
}
