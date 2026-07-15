package com.fail.app.domain.admin.dto.response;

import com.fail.app.domain.report.entity.Report;
import java.time.LocalDateTime;

public record AdminReportResponse(
        Long reportId,
        String targetType,
        Long targetId,
        String reasonType,
        String reasonDetail,
        String status,
        Long reporterUserId,
        LocalDateTime createdAt
) {
    public static AdminReportResponse from(Report report) {
        return new AdminReportResponse(
                report.getId(),
                report.getTargetType().name(),
                report.getTargetId(),
                report.getReasonType().name(),
                report.getReasonDetail(),
                report.getStatus().name(),
                report.getReporter().getId(),
                report.getCreatedAt()
        );
    }
}
