package com.fail.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fail.app.common.exception.ApiException;
import com.fail.app.common.exception.ErrorCode;
import com.fail.app.domain.admin.dto.request.AdminActionRequest;
import com.fail.app.domain.admin.service.AdminService;
import com.fail.app.domain.auth.repository.RefreshTokenRepository;
import com.fail.app.domain.auth.service.RefreshTokenService;
import com.fail.app.domain.category.entity.Category;
import com.fail.app.domain.category.repository.CategoryRepository;
import com.fail.app.domain.moderation.entity.ModerationActionType;
import com.fail.app.domain.moderation.entity.ModerationTargetType;
import com.fail.app.domain.moderation.repository.ModerationActionRepository;
import com.fail.app.domain.post.dto.request.CreatePostRequest;
import com.fail.app.domain.post.dto.response.PostSummaryResponse;
import com.fail.app.domain.post.entity.AdvicePreference;
import com.fail.app.domain.post.entity.FailureSize;
import com.fail.app.domain.post.entity.Post;
import com.fail.app.domain.post.entity.PostSortType;
import com.fail.app.domain.post.entity.PostVisibilityType;
import com.fail.app.domain.post.repository.PostRepository;
import com.fail.app.domain.post.service.PostService;
import com.fail.app.domain.reaction.dto.request.UpsertReactionRequest;
import com.fail.app.domain.reaction.entity.ReactionType;
import com.fail.app.domain.reaction.repository.ReactionRepository;
import com.fail.app.domain.reaction.service.ReactionService;
import com.fail.app.domain.report.dto.request.CreateReportRequest;
import com.fail.app.domain.report.entity.ReportReasonType;
import com.fail.app.domain.report.entity.ReportTargetType;
import com.fail.app.domain.report.repository.ReportRepository;
import com.fail.app.domain.report.service.ReportService;
import com.fail.app.domain.user.entity.User;
import com.fail.app.domain.user.entity.UserRole;
import com.fail.app.domain.user.entity.UserStatus;
import com.fail.app.domain.user.repository.UserRepository;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

class MySqlCoreIntegrationTest extends MySqlContainerIntegrationSupport {

    private static final int CONCURRENT_USER_COUNT = 8;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ReactionRepository reactionRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ModerationActionRepository moderationActionRepository;

    @Autowired
    private ReactionService reactionService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private PostService postService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void flywayCreatesAndValidatesTheSchemaOnMySql() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
        }
        Integer appliedMigrations = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = 1",
                Integer.class
        );
        assertThat(appliedMigrations).isGreaterThanOrEqualTo(6);
    }

    @Test
    void concurrentRefreshRequestsLeaveOneActiveTokenOnMySql() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = saveUser(
                "mysql-refresh-" + suffix + "@example.com",
                "mysql-refresh-" + suffix,
                UserStatus.ACTIVE
        );
        var issued = refreshTokenService.issue(user);
        String familyId = refreshTokenRepository.findTopByUserIdOrderByIdDesc(user.getId())
                .orElseThrow()
                .getFamilyId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<ErrorCode>> results = List.of(
                    executor.submit(() -> rotateAfterStart(start, issued.rawToken())),
                    executor.submit(() -> rotateAfterStart(start, issued.rawToken()))
            );
            start.countDown();

            List<ErrorCode> errors = new ArrayList<>();
            for (Future<ErrorCode> result : results) {
                ErrorCode error = result.get();
                if (error != null) {
                    errors.add(error);
                }
            }

            assertThat(errors).containsExactly(ErrorCode.REFRESH_TOKEN_ALREADY_ROTATED);
            assertThat(refreshTokenRepository.countByFamilyIdAndRevokedAtIsNull(familyId)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentReactionsAndReportsKeepExactAggregateCountsOnMySql() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User author = saveUser("mysql-author-" + suffix + "@example.com", "mysql-author-" + suffix, UserStatus.ACTIVE);
        List<User> users = new ArrayList<>();
        for (int index = 0; index < CONCURRENT_USER_COUNT; index++) {
            users.add(saveUser(
                    "mysql-user-" + suffix + "-" + index + "@example.com",
                    "mysql-user-" + suffix + "-" + index,
                    UserStatus.ACTIVE
            ));
        }
        Category category = saveCategory("mysql-concurrency-" + suffix, true);
        Post reactionPost = savePost(author, category, "mysql concurrent reactions " + suffix);
        Post reportPost = savePost(author, category, "mysql concurrent reports " + suffix);

        runConcurrently(users.stream()
                .map(user -> (Runnable) () -> reactionService.upsertReaction(
                        user.getId(), reactionPost.getId(), new UpsertReactionRequest(ReactionType.ME_TOO)))
                .toList());
        runConcurrently(users.stream()
                .map(user -> (Runnable) () -> reportService.createPostReport(
                        user.getId(),
                        reportPost.getId(),
                        new CreateReportRequest(ReportReasonType.SPAM, "mysql concurrency test")))
                .toList());

        assertThat(reactionRepository.countByPostId(reactionPost.getId())).isEqualTo(CONCURRENT_USER_COUNT);
        assertThat(postRepository.findById(reactionPost.getId()).orElseThrow().getReactionCount())
                .isEqualTo(CONCURRENT_USER_COUNT);
        assertThat(reportRepository.countByTargetTypeAndTargetId(ReportTargetType.POST, reportPost.getId()))
                .isEqualTo(CONCURRENT_USER_COUNT);
        assertThat(postRepository.findById(reportPost.getId()).orElseThrow().getReportCount())
                .isEqualTo(CONCURRENT_USER_COUNT);
    }

    @Test
    void userModerationAndStatePoliciesAreEnforcedOnMySql() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User admin = saveAdmin("mysql-admin-" + suffix + "@example.com", "mysql-admin-" + suffix);
        User target = saveUser("mysql-target-" + suffix + "@example.com", "mysql-target-" + suffix, UserStatus.ACTIVE);

        adminService.restrictUser(admin.getId(), target.getId(), new AdminActionRequest("policy violation"));
        adminService.activateUser(admin.getId(), target.getId());

        var userModerationActions = moderationActionRepository.findAll().stream()
                .filter(action -> action.getTargetId().equals(target.getId()))
                .filter(action -> action.getActionType() == ModerationActionType.RESTRICT_USER
                        || action.getActionType() == ModerationActionType.ACTIVATE_USER)
                .toList();

        assertThat(userModerationActions)
                .hasSize(2)
                .allSatisfy(action -> assertThat(action.getTargetType()).isEqualTo(ModerationTargetType.USER));

        User deletedUser = saveUser(
                "mysql-deleted-" + suffix + "@example.com",
                "mysql-deleted-" + suffix,
                UserStatus.DELETED
        );
        Category activeCategory = saveCategory("mysql-active-" + suffix, true);
        Category inactiveCategory = saveCategory("mysql-inactive-" + suffix, false);

        assertApiError(
                () -> postService.createPost(deletedUser.getId(), createPostRequest(activeCategory.getId())),
                ErrorCode.USER_DELETED
        );
        assertApiError(
                () -> postService.createPost(target.getId(), createPostRequest(inactiveCategory.getId())),
                ErrorCode.CATEGORY_INACTIVE
        );
    }

    @Test
    void postListQueryReturnsVisibleRecordsOnMySql() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User author = saveUser("mysql-list-" + suffix + "@example.com", "mysql-list-" + suffix, UserStatus.ACTIVE);
        Category category = saveCategory("mysql-list-" + suffix, true);
        Post visible = savePost(author, category, "searchable mysql record " + suffix);
        Post hidden = savePost(author, category, "hidden mysql record " + suffix);
        hidden.hide(java.time.LocalDateTime.now());
        postRepository.saveAndFlush(hidden);

        Page<PostSummaryResponse> result = postService.getPosts(
                category.getId(),
                null,
                suffix,
                PostSortType.LATEST,
                PageRequest.of(0, 20)
        );

        assertThat(result.getContent()).extracting(PostSummaryResponse::postId).containsExactly(visible.getId());
    }

    private void runConcurrently(List<Runnable> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Void>> futures = tasks.stream()
                    .map(task -> executor.<Void>submit(() -> {
                        ready.countDown();
                        start.await();
                        task.run();
                        return null;
                    }))
                    .toList();
            ready.await();
            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private void assertApiError(Runnable action, ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }

    private ErrorCode rotateAfterStart(CountDownLatch start, String refreshToken) throws InterruptedException {
        start.await();
        try {
            refreshTokenService.rotate(refreshToken);
            return null;
        } catch (ApiException exception) {
            return exception.getErrorCode();
        }
    }

    private User saveAdmin(String email, String nickname) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .passwordHash("hash")
                .nickname(nickname)
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private User saveUser(String email, String nickname, UserStatus status) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .passwordHash("hash")
                .nickname(nickname)
                .role(UserRole.USER)
                .status(status)
                .build());
    }

    private Category saveCategory(String name, boolean active) {
        return categoryRepository.saveAndFlush(Category.builder()
                .name(name)
                .slug(name)
                .displayOrder(500)
                .isActive(active)
                .build());
    }

    private Post savePost(User author, Category category, String title) {
        return postRepository.saveAndFlush(Post.builder()
                .user(author)
                .category(category)
                .title(title)
                .content("mysql integration test content")
                .visibilityType(PostVisibilityType.NICKNAME)
                .failureSize(FailureSize.SMALL)
                .emotionTag("test")
                .advicePreference(AdvicePreference.ADVICE_OK)
                .retryIntention(true)
                .build());
    }

    private CreatePostRequest createPostRequest(Long categoryId) {
        return new CreatePostRequest(
                categoryId,
                "mysql policy test",
                "mysql policy integration test content",
                PostVisibilityType.NICKNAME,
                FailureSize.SMALL,
                "test",
                AdvicePreference.ADVICE_OK,
                true,
                null
        );
    }
}
