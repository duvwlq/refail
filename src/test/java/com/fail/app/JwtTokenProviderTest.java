package com.fail.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.fail.app.common.config.JwtProperties;
import com.fail.app.common.security.JwtTokenProvider;
import com.fail.app.domain.user.entity.User;
import com.fail.app.domain.user.entity.UserRole;
import com.fail.app.domain.user.entity.UserStatus;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET = "test-jwt-secret-key-that-is-long-enough-for-hs256";

    @Test
    void 설정된_issuer와_다른_토큰은_거부한다() throws Exception {
        JwtTokenProvider issuerA = new JwtTokenProvider(new JwtProperties(SECRET, 60, "issuer-a"));
        JwtTokenProvider issuerB = new JwtTokenProvider(new JwtProperties(SECRET, 60, "issuer-b"));
        User user = User.builder()
                .email("jwt@example.com")
                .passwordHash("hash")
                .nickname("jwt-user")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        Field id = User.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(user, 1L);

        String token = issuerA.createAccessToken(user);

        assertThat(issuerA.validateToken(token)).isTrue();
        assertThat(issuerB.validateToken(token)).isFalse();
    }
}
