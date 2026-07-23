package com.fail.app.domain.auth.service;

import com.fail.app.domain.auth.dto.request.LoginRequest;
import com.fail.app.domain.auth.dto.request.SignupRequest;
import com.fail.app.domain.auth.dto.response.AuthUserResponse;
import com.fail.app.domain.auth.dto.response.LoginResponse;

public interface AuthService {

    AuthUserResponse signup(SignupRequest request);

    IssuedAuthSession login(LoginRequest request);

    AuthUserResponse getCurrentUser(Long userId);

    IssuedAuthSession refresh(String refreshToken);

    void logout(String refreshToken);
}
