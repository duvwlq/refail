package com.fail.app.domain.admin.service;

import com.fail.app.domain.admin.dto.request.AdminActionRequest;
import com.fail.app.domain.admin.dto.request.AdminCategoryRequest;
import com.fail.app.domain.admin.dto.response.AdminActionResponse;
import com.fail.app.domain.admin.dto.response.AdminReportResponse;
import com.fail.app.domain.admin.dto.response.OperationMetricsResponse;
import com.fail.app.domain.auth.service.RefreshTokenService;
import com.fail.app.domain.category.dto.response.CategoryResponse;
import com.fail.app.domain.moderation.entity.ModerationAction;
import com.fail.app.domain.moderation.entity.ModerationActionType;
import com.fail.app.domain.moderation.entity.ModerationTargetType;
import com.fail.app.domain.moderation.repository.ModerationActionRepository;
import com.fail.app.domain.post.entity.Post;
import com.fail.app.domain.report.entity.ReportStatus;
import com.fail.app.domain.report.entity.ReportTargetType;
import com.fail.app.domain.report.repository.ReportRepository;
import com.fail.app.domain.user.entity.User;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final ReportRepository reportRepository;
    private final ModerationActionRepository moderationActionRepository;
    private final RefreshTokenService refreshTokenService;
    private final AdminAccessPolicy adminAccessPolicy;
    private final AdminCategoryManager adminCategoryManager;
    private final AdminMetricsReader adminMetricsReader;
    private final Clock clock;

    @Override
    public Page<AdminReportResponse> getReports(Long adminUserId, ReportStatus status, Pageable pageable) {
        adminAccessPolicy.getAdmin(adminUserId);
        if (status == null) {
            return reportRepository.findAll(pageable).map(AdminReportResponse::from);
        }
        return reportRepository.findAllByStatus(status, pageable).map(AdminReportResponse::from);
    }

    @Override
    @Transactional
    public AdminActionResponse hidePost(Long adminUserId, Long postId, AdminActionRequest request) {
        User admin = adminAccessPolicy.getAdmin(adminUserId);
        Post post = adminAccessPolicy.getPost(postId);
        LocalDateTime now = LocalDateTime.now(clock);
        post.hide(now);
        reportRepository.findAllByTargetTypeAndTargetIdAndStatus(
                        ReportTargetType.POST,
                        postId,
                        ReportStatus.PENDING
                )
                .forEach(report -> report.resolve(admin.getId(), now));
        saveModerationAction(
                admin,
                ModerationTargetType.POST,
                postId,
                ModerationActionType.HIDE_POST,
                request.reason()
        );
        return new AdminActionResponse(postId, true, null, now);
    }

    @Override
    @Transactional
    public AdminActionResponse unhidePost(Long adminUserId, Long postId) {
        User admin = adminAccessPolicy.getAdmin(adminUserId);
        Post post = adminAccessPolicy.getPost(postId);
        LocalDateTime now = LocalDateTime.now(clock);
        post.unhide();
        saveModerationAction(
                admin,
                ModerationTargetType.POST,
                postId,
                ModerationActionType.UNHIDE_POST,
                "게시글 숨김 해제"
        );
        return new AdminActionResponse(postId, false, null, now);
    }

    @Override
    @Transactional
    public AdminActionResponse restrictUser(Long adminUserId, Long userId, AdminActionRequest request) {
        User admin = adminAccessPolicy.getAdmin(adminUserId);
        User user = adminAccessPolicy.getUser(userId);
        LocalDateTime now = LocalDateTime.now(clock);
        user.restrict();
        refreshTokenService.revokeAllForUser(userId);
        saveModerationAction(
                admin,
                ModerationTargetType.USER,
                userId,
                ModerationActionType.RESTRICT_USER,
                request.reason()
        );
        return new AdminActionResponse(userId, false, user.getStatus().name(), now);
    }

    @Override
    @Transactional
    public AdminActionResponse activateUser(Long adminUserId, Long userId) {
        User admin = adminAccessPolicy.getAdmin(adminUserId);
        User user = adminAccessPolicy.getUser(userId);
        LocalDateTime now = LocalDateTime.now(clock);
        user.activate();
        saveModerationAction(
                admin,
                ModerationTargetType.USER,
                userId,
                ModerationActionType.ACTIVATE_USER,
                "사용자 제한 해제"
        );
        return new AdminActionResponse(userId, false, user.getStatus().name(), now);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(Long adminUserId, AdminCategoryRequest request) {
        adminAccessPolicy.getAdmin(adminUserId);
        return adminCategoryManager.create(request);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long adminUserId, Long categoryId, AdminCategoryRequest request) {
        adminAccessPolicy.getAdmin(adminUserId);
        return adminCategoryManager.update(categoryId, request);
    }

    @Override
    @Transactional
    public void deactivateCategory(Long adminUserId, Long categoryId) {
        adminAccessPolicy.getAdmin(adminUserId);
        adminCategoryManager.deactivate(categoryId);
    }

    @Override
    public OperationMetricsResponse getMetrics(Long adminUserId) {
        adminAccessPolicy.getAdmin(adminUserId);
        return adminMetricsReader.getMetrics();
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
