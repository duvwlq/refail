package com.fail.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.fail.app.domain.user.entity.User;
import com.fail.app.domain.user.entity.UserRole;
import com.fail.app.domain.user.entity.UserStatus;
import com.fail.app.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PostListIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostUpdateRepository postUpdateRepository;

    @Test
    void 카테고리와_인기순을_적용하고_노출불가_게시글을_제외한다() throws Exception {
        User user = userRepository.save(User.builder()
                .email("list@example.com")
                .passwordHash("hash")
                .nickname("목록테스터")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        Category daily = saveCategory("일상", "daily-list", 1);
        Category study = saveCategory("공부", "study-list", 2);

        Post popular = postRepository.save(createPost(user, daily, "공감이 많은 글"));
        popular.increaseReactionCount();
        popular.increaseReactionCount();
        Post latest = postRepository.save(createPost(user, daily, "최신 글"));
        postRepository.save(createPost(user, study, "다른 카테고리 글"));
        Post hidden = postRepository.save(createPost(user, daily, "숨김 글"));
        hidden.increaseReactionCount();
        hidden.increaseReactionCount();
        hidden.increaseReactionCount();
        hidden.hide(LocalDateTime.now());
        Post deleted = postRepository.save(createPost(user, daily, "삭제 글"));
        deleted.softDelete(LocalDateTime.now());

        postUpdateRepository.save(PostUpdate.builder()
                .post(popular)
                .user(user)
                .status(PostUpdateStatus.TRYING_AGAIN)
                .content("다시 시도하고 있다.")
                .build());
        postRepository.flush();

        mockMvc.perform(get("/api/v1/posts")
                        .param("sort", "popular")
                        .param("categoryId", daily.getId().toString())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].postId").value(popular.getId()))
                .andExpect(jsonPath("$.content[0].reactionCount").value(2))
                .andExpect(jsonPath("$.content[0].hasUpdates").value(true))
                .andExpect(jsonPath("$.content[1].postId").value(latest.getId()))
                .andExpect(jsonPath("$.content[1].hasUpdates").value(false));
    }

    @Test
    void 지원하지_않는_정렬과_페이지_크기를_거부한다() throws Exception {
        mockMvc.perform(get("/api/v1/posts").param("sort", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));

        mockMvc.perform(get("/api/v1/posts").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }

    private Category saveCategory(String name, String slug, int displayOrder) {
        return categoryRepository.save(Category.builder()
                .name(name)
                .slug(slug)
                .displayOrder(displayOrder)
                .isActive(true)
                .build());
    }

    private Post createPost(User user, Category category, String title) {
        return Post.builder()
                .user(user)
                .category(category)
                .title(title)
                .content("실패를 돌아보고 다음 시도를 기록한다.")
                .visibilityType(PostVisibilityType.NICKNAME)
                .failureSize(FailureSize.SMALL)
                .emotionTag("아쉬움")
                .advicePreference(AdvicePreference.ADVICE_OK)
                .retryIntention(true)
                .build();
    }
}
