package com.fail.app.domain.report.dto.response;

import com.fail.app.domain.report.entity.Report;
import java.time.LocalDateTime;

public record ReportResponse(
        Long reportId,
        Long targetId,
        String status,
        LocalDateTime createdAt
) {
    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getTargetId(),
                report.getStatus().name(),
                report.getCreatedAt()
        );
    }
}
