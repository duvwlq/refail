package com.fail.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fail.app.common.security.JwtTokenProvider;
import com.fail.app.domain.user.entity.User;
import com.fail.app.domain.user.entity.UserRole;
import com.fail.app.domain.user.entity.UserStatus;
import com.fail.app.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ListInputValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Test
    void postListRejectsInvalidPageSearchAndEnumValues() throws Exception {
        assertInvalid(get("/api/v1/posts").param("page", "-1"));
        assertInvalid(get("/api/v1/posts").param("size", "0"));
        assertInvalid(get("/api/v1/posts").param("size", "51"));
        assertInvalid(get("/api/v1/posts").param("keyword", "x".repeat(101)));
        assertInvalid(get("/api/v1/posts").param("sort", "unsupported"));
        assertInvalid(get("/api/v1/posts").param("failureSize", "unsupported"));

        mockMvc.perform(get("/api/v1/posts")
                        .param("page", "0")
                        .param("size", "50")
                        .param("keyword", "x".repeat(100)))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedAndAdminListsUseTheSamePageLimit() throws Exception {
        User user = saveUser("page-user@example.com", "page-user", UserRole.USER);
        User admin = saveUser("page-admin@example.com", "page-admin", UserRole.ADMIN);

        assertInvalid(get("/api/v1/posts/me")
                .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(user))
                .param("size", "51"));
        assertInvalid(get("/api/v1/admin/reports")
                .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(admin))
                .param("size", "51"));
        assertInvalid(get("/api/v1/admin/reports")
                .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(admin))
                .param("status", "unsupported"));
    }

    @Test
    void categorySearchRejectsKeywordLongerThanFiftyCharacters() throws Exception {
        assertInvalid(get("/api/v1/categories").param("keyword", "x".repeat(51)));

        mockMvc.perform(get("/api/v1/categories").param("keyword", "x".repeat(50)))
                .andExpect(status().isOk());
    }

    private void assertInvalid(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    private User saveUser(String email, String nickname, UserRole role) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash("hash")
                .nickname(nickname)
                .role(role)
                .status(UserStatus.ACTIVE)
                .build());
    }
}
