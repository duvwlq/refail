package com.fail.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fail.app.common.security.JwtTokenProvider;
import com.fail.app.domain.category.entity.Category;
import com.fail.app.domain.category.repository.CategoryRepository;
import com.fail.app.domain.post.entity.AdvicePreference;
import com.fail.app.domain.post.entity.FailureSize;
import com.fail.app.domain.post.entity.Post;
import com.fail.app.domain.post.entity.PostUpdate;
import com.fail.app.domain.post.entity.PostUpdateStatus;
import com.fail.app.domain.post.entity.PostVisibilityType;
import com.fail.app.domain.post.repository.PostRepository;
import com.fail.app.domain.post.repository.PostUpdateRepository;
import com.fail.app.domain.report.entity.Report;
import com.fail.app.domain.report.entity.ReportReasonType;
import com.fail.app.domain.report.entity.ReportStatus;
import com.fail.app.domain.report.entity.ReportTargetType;
import com.fail.app.domain.report.repository.ReportRepository;
import com.fail.app.domain.user.entity.User;
import com.fail.app.domain.user.entity.UserRole;
import com.fail.app.domain.user.entity.UserStatus;
import com.fail.app.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@Transactional
class AdminMetricsQueryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostUpdateRepository postUpdateRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void operationMetricsUseOneAggregateQueryAndKeepValues() throws Exception {
        User admin = saveUser("metrics-admin@example.com", "metrics-admin", UserRole.ADMIN);
        User author = saveUser("metrics-author@example.com", "metrics-author", UserRole.USER);
        User reporter = saveUser("metrics-reporter@example.com", "metrics-reporter", UserRole.USER);
        Category category = categoryRepository.save(Category.builder()
                .name("metrics-category")
                .slug("metrics-category")
                .displayOrder(700)
                .isActive(true)
                .build());
        Post first = postRepository.save(createPost(author, category, "first"));
        Post second = postRepository.save(createPost(author, category, "second"));
        postUpdateRepository.save(createUpdate(first, author, PostUpdateStatus.TRYING_AGAIN));
        postUpdateRepository.save(createUpdate(first, author, PostUpdateStatus.STILL_FAILING));
        postUpdateRepository.save(createUpdate(second, author, PostUpdateStatus.SUCCEEDED));
        reportRepository.save(Report.builder()
                .reporter(reporter)
                .targetType(ReportTargetType.POST)
                .targetId(first.getId())
                .reasonType(ReportReasonType.SPAM)
                .reasonDetail("metrics")
                .status(ReportStatus.PENDING)
                .build());
        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        mockMvc.perform(get("/api/v1/admin/metrics")
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPosts").value(2))
                .andExpect(jsonPath("$.postsWithUpdates").value(2))
                .andExpect(jsonPath("$.updateRate").value(100.0))
                .andExpect(jsonPath("$.pendingReports").value(1))
                .andExpect(jsonPath("$.retryingUpdates").value(1))
                .andExpect(jsonPath("$.pausedUpdates").value(1))
                .andExpect(jsonPath("$.succeededUpdates").value(1));

        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(2);
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

    private Post createPost(User user, Category category, String title) {
        return Post.builder()
                .user(user)
                .category(category)
                .title(title)
                .content("metrics content")
                .visibilityType(PostVisibilityType.NICKNAME)
                .failureSize(FailureSize.SMALL)
                .emotionTag("test")
                .advicePreference(AdvicePreference.ADVICE_OK)
                .retryIntention(true)
                .build();
    }

    private PostUpdate createUpdate(Post post, User user, PostUpdateStatus status) {
        return PostUpdate.builder()
                .post(post)
                .user(user)
                .status(status)
                .content("metrics update")
                .build();
    }
}
