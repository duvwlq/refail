package com.fail.app.domain.auth.service;

import com.fail.app.common.exception.ApiException;
import com.fail.app.common.exception.ErrorCode;
import com.fail.app.common.security.JwtTokenProvider;
import com.fail.app.domain.auth.dto.request.LoginRequest;
import com.fail.app.domain.auth.dto.request.SignupRequest;
import com.fail.app.domain.auth.dto.response.AuthUserResponse;
import com.fail.app.domain.auth.dto.response.LoginResponse;
import com.fail.app.domain.user.entity.User;
import com.fail.app.domain.user.entity.UserRole;
import com.fail.app.domain.user.entity.UserStatus;
import com.fail.app.domain.user.repository.UserRepository;
import com.fail.app.domain.user.policy.UserAccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserAccessPolicy userAccessPolicy;

    @Override
    @Transactional
    public AuthUserResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new ApiException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        return AuthUserResponse.from(userRepository.save(user));
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        userAccessPolicy.validateActive(user);

        return new LoginResponse(
                jwtTokenProvider.createAccessToken(user),
                "Bearer",
                AuthUserResponse.from(user)
        );
    }

    @Override
    public AuthUserResponse getCurrentUser(Long userId) {
        return AuthUserResponse.from(userAccessPolicy.getActiveUser(userId));
    }
}
