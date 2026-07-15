package com.fail.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.fail.app.domain.category.entity.Category;
import com.fail.app.domain.category.repository.CategoryRepository;
import com.fail.app.domain.post.entity.AdvicePreference;
import com.fail.app.domain.post.entity.FailureSize;
import com.fail.app.domain.post.entity.Post;
import com.fail.app.domain.post.entity.PostVisibilityType;
import com.fail.app.domain.post.repository.PostRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ReactionReportConcurrencyIntegrationTest {

    private static final int USER_COUNT = 10;

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
    private ReactionService reactionService;

    @Autowired
    private ReportService reportService;

    @Test
    void 동시_공감과_신고에도_행_개수와_집계_값이_일치한다() throws Exception {
        User author = saveUser("concurrency-author@example.com", "concurrency-author");
        List<User> users = new ArrayList<>();
        for (int index = 0; index < USER_COUNT; index++) {
            users.add(saveUser(
                    "concurrency-user-" + index + "@example.com",
                    "concurrency-user-" + index
            ));
        }
        Category category = categoryRepository.save(Category.builder()
                .name("concurrency-category")
                .slug("concurrency-category")
                .displayOrder(999)
                .isActive(true)
                .build());
        Post reactionPost = savePost(author, category, "concurrent reactions");
        Post reportPost = savePost(author, category, "concurrent reports");

        runConcurrently(users.stream()
                .map(user -> (Runnable) () -> reactionService.upsertReaction(
                        user.getId(),
                        reactionPost.getId(),
                        new UpsertReactionRequest(ReactionType.ME_TOO)
                ))
                .toList());
        runConcurrently(users.stream()
                .map(user -> (Runnable) () -> reportService.createPostReport(
                        user.getId(),
                        reportPost.getId(),
                        new CreateReportRequest(ReportReasonType.SPAM, "concurrency test")
                ))
                .toList());

        Post savedReactionPost = postRepository.findById(reactionPost.getId()).orElseThrow();
        Post savedReportPost = postRepository.findById(reportPost.getId()).orElseThrow();
        assertThat(reactionRepository.countByPostId(reactionPost.getId())).isEqualTo(USER_COUNT);
        assertThat(savedReactionPost.getReactionCount()).isEqualTo(USER_COUNT);
        assertThat(reportRepository.countByTargetTypeAndTargetId(ReportTargetType.POST, reportPost.getId()))
                .isEqualTo(USER_COUNT);
        assertThat(savedReportPost.getReportCount()).isEqualTo(USER_COUNT);

        runConcurrently(users.stream()
                .map(user -> (Runnable) () -> reactionService.removeReaction(user.getId(), reactionPost.getId()))
                .toList());

        assertThat(reactionRepository.countByPostId(reactionPost.getId())).isZero();
        assertThat(postRepository.findById(reactionPost.getId()).orElseThrow().getReactionCount()).isZero();
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

    private User saveUser(String email, String nickname) {
        return userRepository.saveAndFlush(User.builder()
                .email(email)
                .passwordHash("hash")
                .nickname(nickname)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private Post savePost(User author, Category category, String title) {
        return postRepository.saveAndFlush(Post.builder()
                .user(author)
                .category(category)
                .title(title)
                .content("concurrency test content")
                .visibilityType(PostVisibilityType.NICKNAME)
                .failureSize(FailureSize.SMALL)
                .emotionTag("test")
                .advicePreference(AdvicePreference.ADVICE_OK)
                .retryIntention(true)
                .build());
    }
}
