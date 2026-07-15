package com.fail.app.domain.report.controller;

import com.fail.app.common.security.CurrentUser;
import com.fail.app.domain.report.dto.request.CreateReportRequest;
import com.fail.app.domain.report.dto.response.ReportResponse;
import com.fail.app.domain.report.service.ReportService;
import com.fail.app.common.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "신고", description = "게시글 신고")
@SecurityRequirement(name = OpenApiConfig.JWT_SCHEME_NAME)
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts/{postId}/reports")
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @Operation(summary = "게시글 신고")
    public ReportResponse createReport(
            @Parameter(hidden = true) CurrentUser currentUser,
            @PathVariable Long postId,
            @Valid @RequestBody CreateReportRequest request
    ) {
        return reportService.createPostReport(currentUser.userId(), postId, request);
    }
}
