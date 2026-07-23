package com.fail.app.domain.auth.controller;

import com.fail.app.common.security.CurrentUser;
import com.fail.app.domain.auth.dto.request.LoginRequest;
import com.fail.app.domain.auth.dto.request.SignupRequest;
import com.fail.app.domain.auth.dto.response.AuthUserResponse;
import com.fail.app.domain.auth.dto.response.LoginResponse;
import com.fail.app.domain.auth.service.AuthService;
import com.fail.app.common.config.OpenApiConfig;
import com.fail.app.common.config.JwtProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;

@RestController
@Tag(name = "인증", description = "회원가입, 로그인, 현재 사용자 조회")
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    public static final String REFRESH_COOKIE_NAME = "refail_refresh";

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @PostMapping("/signup")
    @Operation(summary = "회원가입")
    public AuthUserResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "성공하면 API 인증에 사용할 JWT 액세스 토큰을 반환합니다.")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return sessionResponse(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "액세스 토큰 갱신", description = "HttpOnly 쿠키의 리프레시 토큰을 회전하고 새 액세스 토큰을 반환합니다.")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken
    ) {
        return sessionResponse(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 리프레시 토큰을 폐기하고 쿠키를 제거합니다.")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken
    ) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", java.time.Duration.ZERO).toString())
                .build();
    }

    @GetMapping("/me")
    @Operation(summary = "현재 사용자 조회")
    @SecurityRequirement(name = OpenApiConfig.JWT_SCHEME_NAME)
    public AuthUserResponse me(@Parameter(hidden = true) CurrentUser currentUser) {
        return authService.getCurrentUser(currentUser.userId());
    }

    private ResponseEntity<LoginResponse> sessionResponse(
            com.fail.app.domain.auth.service.IssuedAuthSession session
    ) {
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie(session.refreshToken(), session.refreshTokenMaxAge()).toString()
                )
                .body(session.response());
    }

    private ResponseCookie refreshCookie(String token, java.time.Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(jwtProperties.refreshCookieSecure())
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(maxAge)
                .build();
    }
}
