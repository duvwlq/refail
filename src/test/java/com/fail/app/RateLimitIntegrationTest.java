package com.fail.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fail.app.common.security.JwtTokenProvider;
import com.fail.app.common.config.RateLimitProperties;
import com.fail.app.domain.category.entity.Category;
import com.fail.app.domain.category.repository.CategoryRepository;
import com.fail.app.domain.post.entity.AdvicePreference;
import com.fail.app.domain.post.entity.FailureSize;
import com.fail.app.domain.post.entity.Post;
import com.fail.app.domain.post.entity.PostVisibilityType;
import com.fail.app.domain.post.repository.PostRepository;
import com.fail.app.domain.user.entity.User;
import com.fail.app.domain.user.entity.UserRole;
import com.fail.app.domain.user.entity.UserStatus;
import com.fail.app.domain.user.repository.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "app.rate-limit.login.limit=2",
        "app.rate-limit.signup.limit=1",
        "app.rate-limit.refresh.limit=2",
        "app.rate-limit.report.limit=1",
        "app.rate-limit.login.window-seconds=60",
        "app.rate-limit.signup.window-seconds=60",
        "app.rate-limit.refresh.window-seconds=60",
        "app.rate-limit.report.window-seconds=60"
})
@AutoConfigureMockMvc
@Transactional
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private RateLimitProperties rateLimitProperties;

    @Test
    void loginSignupAndRefreshAreLimitedByClientIp() throws Exception {
        assertThat(rateLimitProperties.enabled()).isTrue();
        assertThat(rateLimitProperties.login().limit()).isEqualTo(2);
        assertThat(rateLimitProperties.signup().limit()).isEqualTo(1);
        assertThat(rateLimitProperties.refresh().limit()).isEqualTo(2);
        String loginBody = """
                {"email":"none@example.com","password":"password123"}
                """;
        for (int index = 0; index < 2; index++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(remoteAddress("198.51.100.10"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody))
                    .andExpect(status().isUnauthorized());
        }
        assertRateLimited(post("/api/v1/auth/login")
                .with(remoteAddress("198.51.100.10"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .with(remoteAddress("198.51.100.11"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"rate-one@example.com","password":"password123","nickname":"rate-one"}
                                """))
                .andExpect(status().isOk());
        assertRateLimited(post("/api/v1/auth/signup")
                .with(remoteAddress("198.51.100.11"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"rate-two@example.com","password":"password123","nickname":"rate-two"}
                        """));

        for (int index = 0; index < 2; index++) {
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .with(remoteAddress("198.51.100.12")))
                    .andExpect(status().isUnauthorized());
        }
        assertRateLimited(post("/api/v1/auth/refresh")
                .with(remoteAddress("198.51.100.12")));
    }

    @Test
    void reportsAreLimitedByAuthenticatedUser() throws Exception {
        assertThat(rateLimitProperties.report().limit()).isEqualTo(1);
        User author = saveUser("rate-author@example.com", "rate-author");
        User reporter = saveUser("rate-reporter@example.com", "rate-reporter");
        Category category = categoryRepository.save(Category.builder()
                .name("rate-category")
                .slug("rate-category")
                .displayOrder(800)
                .isActive(true)
                .build());
        Post first = postRepository.save(createPost(author, category, "first"));
        Post second = postRepository.save(createPost(author, category, "second"));
        String token = jwtTokenProvider.createAccessToken(reporter);
        String reportBody = """
                {"reasonType":"SPAM","reasonDetail":"반복 신고 제한 테스트"}
                """;

        mockMvc.perform(post("/api/v1/posts/{postId}/reports", first.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportBody))
                .andExpect(status().isOk());

        assertRateLimited(post("/api/v1/posts/{postId}/reports", second.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reportBody));

        assertThat(meterRegistry.find("refail.rate_limit.exceeded").counters())
                .anySatisfy(counter -> assertThat(counter.count()).isGreaterThanOrEqualTo(1));
    }

    private void assertRateLimited(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request
    ) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("RATE_001"));
    }

    private RequestPostProcessor remoteAddress(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    private User saveUser(String email, String nickname) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash("hash")
                .nickname(nickname)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private Post createPost(User user, Category category, String title) {
        return Post.builder()
                .user(user)
                .category(category)
                .title(title)
                .content("rate limit content")
                .visibilityType(PostVisibilityType.NICKNAME)
                .failureSize(FailureSize.SMALL)
                .emotionTag("test")
                .advicePreference(AdvicePreference.ADVICE_OK)
                .retryIntention(true)
                .build();
    }
}
