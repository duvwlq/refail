package com.fail.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fail.app.common.security.JwtTokenProvider;
import com.fail.app.domain.category.entity.Category;
import com.fail.app.domain.category.repository.CategoryRepository;
import com.fail.app.domain.moderation.repository.ModerationActionRepository;
import com.fail.app.domain.post.entity.AdvicePreference;
import com.fail.app.domain.post.entity.FailureSize;
import com.fail.app.domain.post.entity.Post;
import com.fail.app.domain.post.entity.PostVisibilityType;
import com.fail.app.domain.post.entity.PostUpdate;
import com.fail.app.domain.post.entity.PostUpdateStatus;
import com.fail.app.domain.post.repository.PostRepository;
import com.fail.app.domain.post.repository.PostUpdateRepository;
import com.fail.app.domain.report.entity.ReportStatus;
import com.fail.app.domain.report.repository.ReportRepository;
import com.fail.app.domain.user.entity.User;
import com.fail.app.domain.user.entity.UserRole;
import com.fail.app.domain.user.entity.UserStatus;
import com.fail.app.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminReportModerationIntegrationTest {

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
    private ModerationActionRepository moderationActionRepository;

    @Test
    void 관리자가_신고를_조회하고_게시글을_숨김_처리한다() throws Exception {
        User author = saveUser("admin-flow-author@example.com", "글작성자", UserRole.USER);
        User reporter = saveUser("admin-flow-reporter@example.com", "신고자", UserRole.USER);
        User admin = saveUser("admin@example.com", "관리자", UserRole.ADMIN);
        Category category = categoryRepository.save(Category.builder()
                .name("관리 테스트")
                .slug("admin-test")
                .displayOrder(20)
                .isActive(true)
                .build());
        Post post = postRepository.save(Post.builder()
                .user(author)
                .category(category)
                .title("신고 대상 게시글")
                .content("관리자 숨김 흐름을 검증한다.")
                .visibilityType(PostVisibilityType.NICKNAME)
                .failureSize(FailureSize.SMALL)
                .emotionTag("아쉬움")
                .advicePreference(AdvicePreference.ADVICE_OK)
                .retryIntention(true)
                .build());
        postUpdateRepository.save(PostUpdate.builder()
                .post(post)
                .user(author)
                .status(PostUpdateStatus.TRYING_AGAIN)
                .content("숨김 게시글에서는 노출되면 안 되는 기록")
                .build());
        String reporterToken = jwtTokenProvider.createAccessToken(reporter);
        String adminToken = jwtTokenProvider.createAccessToken(admin);

        mockMvc.perform(post("/api/v1/posts/{postId}/reports", post.getId())
                        .header("Authorization", "Bearer " + reporterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reasonType\":\"ABUSE\",\"reasonDetail\":\"공격적인 표현\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
        Long reportId = reportRepository.findAll().getFirst().getId();

        mockMvc.perform(get("/api/v1/admin/reports")
                        .header("Authorization", "Bearer " + reporterToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_002"));

        mockMvc.perform(get("/api/v1/admin/reports")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].reportId").value(reportId))
                .andExpect(jsonPath("$.content[0].targetId").value(post.getId()));

        mockMvc.perform(patch("/api/v1/admin/posts/{postId}/hide", post.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"운영 정책 위반\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetId").value(post.getId()))
                .andExpect(jsonPath("$.hidden").value(true));

        assertThat(reportRepository.findById(reportId).orElseThrow().getStatus())
                .isEqualTo(ReportStatus.RESOLVED);
        assertThat(reportRepository.findById(reportId).orElseThrow().getProcessedBy())
                .isEqualTo(admin.getId());
        mockMvc.perform(get("/api/v1/posts/{postId}", post.getId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/posts/{postId}/updates", post.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_001"));
        mockMvc.perform(get("/api/v1/posts").param("categoryId", category.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/v1/admin/reports")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(patch("/api/v1/admin/posts/{postId}/unhide", post.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hidden").value(false));
        mockMvc.perform(get("/api/v1/posts/{postId}", post.getId()))
                .andExpect(status().isOk());
        assertThat(moderationActionRepository.count()).isEqualTo(2);
    }

    @Test
    void 제한된_관리자의_기존_JWT는_관리자_API에_접근할_수_없다() throws Exception {
        User admin = saveUser("restricted-admin@example.com", "제한관리자", UserRole.ADMIN);
        String adminToken = jwtTokenProvider.createAccessToken(admin);
        admin.restrict();
        userRepository.flush();

        mockMvc.perform(get("/api/v1/admin/metrics")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_002"));

        mockMvc.perform(get("/api/v1/admin/reports")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_002"));
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
