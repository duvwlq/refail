package com.fail.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class PostQueryPerformanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void postListQueryCountDoesNotGrowWithPostCount() throws Exception {
        User user = userRepository.save(User.builder()
                .email("query-performance@example.com")
                .passwordHash("hash")
                .nickname("query-performance-user")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        Category category = categoryRepository.save(Category.builder()
                .name("query-performance-category")
                .slug("query-performance-category")
                .displayOrder(100)
                .isActive(true)
                .build());

        for (int index = 0; index < 10; index++) {
            postRepository.save(Post.builder()
                    .user(user)
                    .category(category)
                    .title("query performance post " + index)
                    .content("The number of SQL statements must stay constant.")
                    .visibilityType(PostVisibilityType.NICKNAME)
                    .failureSize(FailureSize.SMALL)
                    .emotionTag("test")
                    .advicePreference(AdvicePreference.ADVICE_OK)
                    .retryIntention(true)
                    .build());
        }

        entityManager.flush();
        entityManager.clear();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        mockMvc.perform(get("/api/v1/posts").param("size", "20"))
                .andExpect(status().isOk());

        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }
}
