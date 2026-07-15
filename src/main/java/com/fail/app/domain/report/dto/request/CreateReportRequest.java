package com.fail.app.domain.report.dto.request;

import com.fail.app.domain.report.entity.ReportReasonType;
import jakarta.validation.constraints.NotNull;

public record CreateReportRequest(
        @NotNull ReportReasonType reasonType,
        String reasonDetail
) {
}
