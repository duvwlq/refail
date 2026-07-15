package com.fail.app.domain.report.service;

import com.fail.app.common.exception.ApiException;
import com.fail.app.common.exception.ErrorCode;
import com.fail.app.domain.post.entity.Post;
import com.fail.app.domain.post.repository.PostRepository;
import com.fail.app.domain.report.dto.request.CreateReportRequest;
import com.fail.app.domain.report.dto.response.ReportResponse;
import com.fail.app.domain.report.entity.Report;
import com.fail.app.domain.report.entity.ReportStatus;
import com.fail.app.domain.report.entity.ReportTargetType;
import com.fail.app.domain.report.repository.ReportRepository;
import com.fail.app.domain.user.entity.User;
import com.fail.app.domain.user.policy.UserAccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final UserAccessPolicy userAccessPolicy;

    @Override
    @Transactional
    public ReportResponse createPostReport(Long userId, Long postId, CreateReportRequest request) {
        User reporter = getActiveUser(userId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(ErrorCode.POST_NOT_FOUND));
        if (post.isDeleted() || post.isHidden()) {
            throw new ApiException(ErrorCode.POST_NOT_FOUND);
        }

        reportRepository.findByReporterIdAndTargetTypeAndTargetId(userId, ReportTargetType.POST, postId)
                .ifPresent(existing -> {
                    throw new ApiException(ErrorCode.DUPLICATE_REPORT);
                });

        Report report = Report.builder()
                .reporter(reporter)
                .targetType(ReportTargetType.POST)
                .targetId(post.getId())
                .reasonType(request.reasonType())
                .reasonDetail(request.reasonDetail())
                .status(ReportStatus.PENDING)
                .build();

        postRepository.incrementReportCount(postId);
        Report savedReport = reportRepository.save(report);
        return ReportResponse.from(savedReport);
    }

    private User getActiveUser(Long userId) {
        return userAccessPolicy.getActiveUser(userId);
    }
}
