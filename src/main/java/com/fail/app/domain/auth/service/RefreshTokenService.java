package com.fail.app.domain.auth.service;

import com.fail.app.common.config.JwtProperties;
import com.fail.app.common.exception.ApiException;
import com.fail.app.common.exception.ErrorCode;
import com.fail.app.domain.auth.entity.RefreshToken;
import com.fail.app.domain.auth.repository.RefreshTokenRepository;
import com.fail.app.domain.user.entity.User;
import com.fail.app.domain.user.policy.UserAccessPolicy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final UserAccessPolicy userAccessPolicy;

    @Transactional
    public IssuedRefreshToken issue(User user) {
        return issue(user, UUID.randomUUID().toString());
    }

    @Transactional(noRollbackFor = ApiException.class)
    public RotatedRefreshToken rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ApiException(ErrorCode.REFRESH_TOKEN_REQUIRED);
        }

        LocalDateTime now = LocalDateTime.now();
        RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (current.isExpired(now)) {
            current.revoke(now);
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (current.isRevoked()) {
            boolean concurrentRetry = current.getReplacedByTokenHash() != null
                    && current.getRevokedAt().isAfter(now.minusSeconds(jwtProperties.refreshReuseGraceSeconds()));
            if (!concurrentRetry) {
                refreshTokenRepository.revokeActiveFamily(current.getFamilyId(), now);
                throw new ApiException(ErrorCode.REFRESH_TOKEN_REUSED);
            }
            throw new ApiException(ErrorCode.REFRESH_TOKEN_ALREADY_ROTATED);
        }

        User user = userAccessPolicy.getActiveUser(current.getUser().getId());
        String replacementRaw = generateRawToken();
        String replacementHash = hash(replacementRaw);
        current.rotate(now, replacementHash);
        RefreshToken replacement = refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(replacementHash)
                .familyId(current.getFamilyId())
                .expiresAt(now.plusSeconds(jwtProperties.refreshTokenValiditySeconds()))
                .build());

        return new RotatedRefreshToken(user, replacementRaw, maxAge(), replacement.getExpiresAt());
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHashForUpdate(hash(rawToken))
                .ifPresent(token -> token.revoke(LocalDateTime.now()));
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeActiveByUserId(userId, LocalDateTime.now());
    }

    private IssuedRefreshToken issue(User user, String familyId) {
        String rawToken = generateRawToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(jwtProperties.refreshTokenValiditySeconds());
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .familyId(familyId)
                .expiresAt(expiresAt)
                .build());
        return new IssuedRefreshToken(rawToken, maxAge(), expiresAt);
    }

    private Duration maxAge() {
        return Duration.ofSeconds(jwtProperties.refreshTokenValiditySeconds());
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }

    public record IssuedRefreshToken(String rawToken, Duration maxAge, LocalDateTime expiresAt) {
    }

    public record RotatedRefreshToken(
            User user,
            String rawToken,
            Duration maxAge,
            LocalDateTime expiresAt
    ) {
    }
}
