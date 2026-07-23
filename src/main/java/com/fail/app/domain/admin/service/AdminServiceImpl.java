package com.fail.app.domain.admin.service;

import com.fail.app.common.exception.ApiException;
import com.fail.app.common.exception.ErrorCode;
import com.fail.app.domain.admin.dto.request.AdminActionRequest;
import com.fail.app.domain.admin.dto.response.AdminActionResponse;
import com.fail.app.domain.admin.dto.response.AdminReportResponse;
import com.fail.app.domain.moderation.entity.ModerationAction;
import com.fail.app.domain.moderation.entity.ModerationActionType;
import com.fail.app.domain.moderation.entity.ModerationTargetType;
import com.fail.app.domain.moderation.repository.ModerationActionRepository;
import com.fail.app.domain.post.entity.Post;
import com.fail.app.domain.post.repository.PostRepository;
import com.fail.app.domain.report.entity.ReportStatus;
import com.fail.app.domain.report.entity.ReportTargetType;
import com.fail.app.domain.report.repository.ReportRepository;
import com.fail.app.domain.user.entity.User;
import com.fail.app.domain.user.entity.UserRole;
import com.fail.app.domain.user.repository.UserRepository;
import com.fail.app.domain.user.policy.UserAccessPolicy;
import com.fail.app.domain.admin.dto.request.AdminCategoryRequest;
import com.fail.app.domain.category.dto.response.CategoryResponse;
import com.fail.app.domain.category.entity.Category;
import com.fail.app.domain.category.repository.CategoryRepository;
import com.fail.app.domain.admin.dto.response.OperationMetricsResponse;
import com.fail.app.domain.post.repository.PostUpdateRepository;
import com.fail.app.domain.post.entity.PostUpdateStatus;
import com.fail.app.domain.auth.service.RefreshTokenService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ModerationActionRepository moderationActionRepository;
    private final CategoryRepository categoryRepository;
    private final PostUpdateRepository postUpdateRepository;
    private final UserAccessPolicy userAccessPolicy;
    private final RefreshTokenService refreshTokenService;

    @Override
    public Page<AdminReportResponse> getReports(Long adminUserId, ReportStatus status, Pageable pageable) {
        getAdmin(adminUserId);
        if (status == null) {
            return reportRepository.findAll(pageable).map(AdminReportResponse::from);
        }
        return reportRepository.findAllByStatus(status, pageable).map(AdminReportResponse::from);
    }

    @Override
    @Transactional
    public AdminActionResponse hidePost(Long adminUserId, Long postId, AdminActionRequest request) {
        User admin = getAdmin(adminUserId);
        Post post = getPost(postId);
        LocalDateTime now = LocalDateTime.now();
        post.hide(now);
        reportRepository.findAllByTargetTypeAndTargetIdAndStatus(
                        ReportTargetType.POST,
                        postId,
                        ReportStatus.PENDING
                )
                .forEach(report -> report.resolve(admin.getId(), now));
        saveModerationAction(admin, ModerationTargetType.POST, postId, ModerationActionType.HIDE_POST, request.reason());
        return new AdminActionResponse(postId, true, null, now);
    }

    @Override
    @Transactional
    public AdminActionResponse unhidePost(Long adminUserId, Long postId) {
        User admin = getAdmin(adminUserId);
        Post post = getPost(postId);
        LocalDateTime now = LocalDateTime.now();
        post.unhide();
        saveModerationAction(admin, ModerationTargetType.POST, postId, ModerationActionType.UNHIDE_POST, "숨김 해제");
        return new AdminActionResponse(postId, false, null, now);
    }

    @Override
    @Transactional
    public AdminActionResponse restrictUser(Long adminUserId, Long userId, AdminActionRequest request) {
        User admin = getAdmin(adminUserId);
        User user = getUser(userId);
        LocalDateTime now = LocalDateTime.now();
        user.restrict();
        refreshTokenService.revokeAllForUser(userId);
        saveModerationAction(admin, ModerationTargetType.USER, userId, ModerationActionType.RESTRICT_USER, request.reason());
        return new AdminActionResponse(userId, false, user.getStatus().name(), now);
    }

    @Override
    @Transactional
    public AdminActionResponse activateUser(Long adminUserId, Long userId) {
        User admin = getAdmin(adminUserId);
        User user = getUser(userId);
        LocalDateTime now = LocalDateTime.now();
        user.activate();
        saveModerationAction(admin, ModerationTargetType.USER, userId, ModerationActionType.ACTIVATE_USER, "사용자 활성화");
        return new AdminActionResponse(userId, false, user.getStatus().name(), now);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "activeCategories", allEntries = true)
    public CategoryResponse createCategory(Long adminUserId, AdminCategoryRequest request) {
        getAdmin(adminUserId);
        Category category = Category.builder().name(request.name()).slug(request.slug())
                .displayOrder(request.displayOrder()).isActive(true).build();
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "activeCategories", allEntries = true)
    public CategoryResponse updateCategory(Long adminUserId, Long categoryId, AdminCategoryRequest request) {
        getAdmin(adminUserId);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND));
        category.update(request.name(), request.slug(), request.displayOrder());
        return CategoryResponse.from(category);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "activeCategories", allEntries = true)
    public void deactivateCategory(Long adminUserId, Long categoryId) {
        getAdmin(adminUserId);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND));
        category.deactivate();
    }

    @Override
    public OperationMetricsResponse getMetrics(Long adminUserId) {
        getAdmin(adminUserId);
        long total = postRepository.countByDeletedAtIsNull();
        long withUpdates = postUpdateRepository.countPostsWithUpdates();
        return new OperationMetricsResponse(
                total, withUpdates, total == 0 ? 0 : Math.round(withUpdates * 1000.0 / total) / 10.0,
                reportRepository.countByStatus(ReportStatus.PENDING),
                postUpdateRepository.countByStatusAndDeletedAtIsNull(PostUpdateStatus.TRYING_AGAIN),
                postUpdateRepository.countByStatusAndDeletedAtIsNull(PostUpdateStatus.STILL_FAILING),
                postUpdateRepository.countByStatusAndDeletedAtIsNull(PostUpdateStatus.SUCCEEDED)
        );
    }

    private User getAdmin(Long adminUserId) {
        User user = userAccessPolicy.getActiveUser(adminUserId);
        if (user.getRole() != UserRole.ADMIN) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        return user;
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    private Post getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(ErrorCode.POST_NOT_FOUND));
    }

    private void saveModerationAction(
            User admin,
            ModerationTargetType targetType,
            Long targetId,
            ModerationActionType actionType,
            String reason
    ) {
        moderationActionRepository.save(ModerationAction.builder()
                .adminUser(admin)
                .targetType(targetType)
                .targetId(targetId)
                .actionType(actionType)
                .reason(reason)
                .build());
    }
}
