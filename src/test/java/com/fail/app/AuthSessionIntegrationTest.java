package com.fail.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fail.app.domain.auth.repository.RefreshTokenRepository;
import com.fail.app.domain.user.entity.User;
import com.fail.app.domain.user.entity.UserRole;
import com.fail.app.domain.user.entity.UserStatus;
import com.fail.app.domain.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSessionIntegrationTest {

    private static final String REFRESH_COOKIE = "refail_refresh";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void loginRefreshRotationAndLogoutManageTheSessionLifecycle() throws Exception {
        saveUser(UserStatus.ACTIVE);

        MvcResult login = login()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(cookie().httpOnly(REFRESH_COOKIE, true))
                .andReturn();

        Cookie firstCookie = login.getResponse().getCookie(REFRESH_COOKIE);
        assertThat(firstCookie).isNotNull();
        assertThat(refreshTokenRepository.findAll())
                .singleElement()
                .satisfies(token -> assertThat(token.getTokenHash()).isNotEqualTo(firstCookie.getValue()));

        MvcResult refresh = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(firstCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(cookie().httpOnly(REFRESH_COOKIE, true))
                .andReturn();

        Cookie rotatedCookie = refresh.getResponse().getCookie(REFRESH_COOKIE);
        assertThat(rotatedCookie).isNotNull();
        assertThat(rotatedCookie.getValue()).isNotEqualTo(firstCookie.getValue());

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(firstCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_005"));

        mockMvc.perform(post("/api/v1/auth/logout").cookie(rotatedCookie))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(REFRESH_COOKIE, 0));

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(rotatedCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void restrictedUserCannotRefresh() throws Exception {
        User user = saveUser(UserStatus.ACTIVE);
        Cookie refreshCookie = login().andReturn().getResponse().getCookie(REFRESH_COOKIE);
        user.restrict();
        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_002"));
    }

    @Test
    void missingRefreshCookieIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_004"));
    }

    @Test
    void signupRejectsDuplicateIdentityAndLoginRejectsInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "identity@example.com",
                                  "password": "password123",
                                  "nickname": "identity-user"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "identity@example.com",
                                  "password": "password123",
                                  "nickname": "another-user"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_003"));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "another@example.com",
                                  "password": "password123",
                                  "nickname": "identity-user"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_004"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "identity@example.com",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_003"));
    }

    private org.springframework.test.web.servlet.ResultActions login() throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "session@example.com",
                                  "password": "password123"
                                }
                                """));
    }

    private User saveUser(UserStatus status) {
        return userRepository.saveAndFlush(User.builder()
                .email("session@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .nickname("session-user")
                .role(UserRole.USER)
                .status(status)
                .build());
    }
}
