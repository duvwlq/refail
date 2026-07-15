package com.fail.app.domain.report.service;

import com.fail.app.domain.report.dto.request.CreateReportRequest;
import com.fail.app.domain.report.dto.response.ReportResponse;

public interface ReportService {

    ReportResponse createPostReport(Long userId, Long postId, CreateReportRequest request);
}
