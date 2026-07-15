package com.fail.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fail.app.common.security.JwtTokenProvider;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserCategoryPolicyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    @Test
    void deletedUserCannotLoginUseExistingTokenOrWrite() throws Exception {
        User deletedUser = userRepository.save(User.builder()
                .email("deleted-user@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .nickname("deleted-user")
                .role(UserRole.USER)
                .status(UserStatus.DELETED)
                .build());
        Category category = saveCategory("active-category", "active-category", true);
        String token = jwtTokenProvider.createAccessToken(deletedUser);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"deleted-user@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_005"));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_005"));

        mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPostBody(category.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_005"));
    }

    @Test
    void inactiveCategoryRejectsNewClassificationButKeepsExistingPostVisible() throws Exception {
        User user = saveActiveUser("category-policy@example.com", "category-policy");
        Category activeCategory = saveCategory("before-deactivation", "before-deactivation", true);
        Category otherCategory = saveCategory("other-active", "other-active", true);
        Post existingPost = postRepository.save(Post.builder()
                .user(user)
                .category(activeCategory)
                .title("existing failure record")
                .content("this record remains readable after category deactivation")
                .visibilityType(PostVisibilityType.NICKNAME)
                .failureSize(FailureSize.SMALL)
                .emotionTag("calm")
                .advicePreference(AdvicePreference.ADVICE_OK)
                .retryIntention(true)
                .build());
        activeCategory.deactivate();
        categoryRepository.flush();
        String token = jwtTokenProvider.createAccessToken(user);

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.categoryId == %d)]".formatted(activeCategory.getId())).doesNotExist());

        mockMvc.perform(post("/api/v1/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPostBody(activeCategory.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_002"));

        mockMvc.perform(patch("/api/v1/posts/{postId}", existingPost.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePostBody(activeCategory.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_002"));

        mockMvc.perform(get("/api/v1/posts/{postId}", existingPost.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(existingPost.getId()));

        mockMvc.perform(patch("/api/v1/posts/{postId}", existingPost.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePostBody(otherCategory.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(otherCategory.getId()));
    }

    private User saveActiveUser(String email, String nickname) {
        return userRepository.save(User.builder()
                .email(email)
                .passwordHash("hash")
                .nickname(nickname)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private Category saveCategory(String name, String slug, boolean active) {
        return categoryRepository.save(Category.builder()
                .name(name)
                .slug(slug)
                .displayOrder(100)
                .isActive(active)
                .build());
    }

    private String createPostBody(Long categoryId) {
        return """
                {
                  "categoryId": %d,
                  "title": "new failure record",
                  "content": "a sufficiently descriptive failure record",
                  "visibilityType": "NICKNAME",
                  "failureSize": "SMALL",
                  "emotionTag": "calm",
                  "advicePreference": "ADVICE_OK",
                  "retryIntention": true
                }
                """.formatted(categoryId);
    }

    private String updatePostBody(Long categoryId) {
        return """
                {
                  "categoryId": %d,
                  "title": "updated failure record",
                  "content": "an updated and sufficiently descriptive failure record",
                  "failureSize": "MEDIUM",
                  "emotionTag": "focused",
                  "advicePreference": "ADVICE_OK",
                  "retryIntention": true
                }
                """.formatted(categoryId);
    }
}
