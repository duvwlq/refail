package com.fail.app.domain.auth.controller;

import com.fail.app.common.security.CurrentUser;
import com.fail.app.domain.auth.dto.request.LoginRequest;
import com.fail.app.domain.auth.dto.request.SignupRequest;
import com.fail.app.domain.auth.dto.response.AuthUserResponse;
import com.fail.app.domain.auth.dto.response.LoginResponse;
import com.fail.app.domain.auth.service.AuthService;
import com.fail.app.common.config.OpenApiConfig;
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

@RestController
@Tag(name = "인증", description = "회원가입, 로그인, 현재 사용자 조회")
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "회원가입")
    public AuthUserResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "성공하면 API 인증에 사용할 JWT 액세스 토큰을 반환합니다.")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "현재 사용자 조회")
    @SecurityRequirement(name = OpenApiConfig.JWT_SCHEME_NAME)
    public AuthUserResponse me(@Parameter(hidden = true) CurrentUser currentUser) {
        return authService.getCurrentUser(currentUser.userId());
    }
}
