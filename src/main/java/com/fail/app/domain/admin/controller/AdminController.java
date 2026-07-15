package com.fail.app.domain.admin.controller;

import com.fail.app.common.security.CurrentUser;
import com.fail.app.domain.admin.dto.request.AdminActionRequest;
import com.fail.app.domain.admin.dto.response.AdminActionResponse;
import com.fail.app.domain.admin.dto.response.AdminReportResponse;
import com.fail.app.domain.admin.service.AdminService;
import com.fail.app.domain.report.entity.ReportStatus;
import com.fail.app.common.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import com.fail.app.common.web.ListQueryPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import com.fail.app.domain.admin.dto.request.AdminCategoryRequest;
import com.fail.app.domain.category.dto.response.CategoryResponse;
import com.fail.app.domain.admin.dto.response.OperationMetricsResponse;

@RestController
@Tag(name = "관리자", description = "신고 조회와 운영 조치")
@SecurityRequirement(name = OpenApiConfig.JWT_SCHEME_NAME)
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/metrics")
    @Operation(summary = "운영 지표 조회")
    public OperationMetricsResponse getMetrics(@Parameter(hidden = true) CurrentUser currentUser) {
        return adminService.getMetrics(currentUser.userId());
    }

    @GetMapping("/reports")
    @Operation(summary = "신고 목록 조회")
    public Page<AdminReportResponse> getReports(
            @Parameter(hidden = true) CurrentUser currentUser,
            @RequestParam(required = false) ReportStatus status,
            @Parameter(schema = @Schema(minimum = "0", defaultValue = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(schema = @Schema(minimum = "1", maximum = "50", defaultValue = "20"))
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminService.getReports(
                currentUser.userId(),
                status,
                ListQueryPolicy.pageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    @PatchMapping("/posts/{postId}/hide")
    @Operation(summary = "게시글 숨김")
    public AdminActionResponse hidePost(
            @Parameter(hidden = true) CurrentUser currentUser,
            @PathVariable Long postId,
            @Valid @RequestBody AdminActionRequest request
    ) {
        return adminService.hidePost(currentUser.userId(), postId, request);
    }

    @PatchMapping("/posts/{postId}/unhide")
    @Operation(summary = "게시글 숨김 해제")
    public AdminActionResponse unhidePost(
            @Parameter(hidden = true) CurrentUser currentUser,
            @PathVariable Long postId
    ) {
        return adminService.unhidePost(currentUser.userId(), postId);
    }

    @PatchMapping("/users/{userId}/restrict")
    @Operation(summary = "사용자 활동 제한")
    public AdminActionResponse restrictUser(
            @Parameter(hidden = true) CurrentUser currentUser,
            @PathVariable Long userId,
            @Valid @RequestBody AdminActionRequest request
    ) {
        return adminService.restrictUser(currentUser.userId(), userId, request);
    }

    @PatchMapping("/users/{userId}/activate")
    @Operation(summary = "사용자 활동 제한 해제")
    public AdminActionResponse activateUser(
            @Parameter(hidden = true) CurrentUser currentUser,
            @PathVariable Long userId
    ) {
        return adminService.activateUser(currentUser.userId(), userId);
    }

    @PostMapping("/categories")
    @Operation(summary = "카테고리 생성")
    public CategoryResponse createCategory(@Parameter(hidden = true) CurrentUser currentUser,
            @Valid @RequestBody AdminCategoryRequest request) {
        return adminService.createCategory(currentUser.userId(), request);
    }

    @PatchMapping("/categories/{categoryId}")
    @Operation(summary = "카테고리 수정")
    public CategoryResponse updateCategory(@Parameter(hidden = true) CurrentUser currentUser,
            @PathVariable Long categoryId, @Valid @RequestBody AdminCategoryRequest request) {
        return adminService.updateCategory(currentUser.userId(), categoryId, request);
    }

    @DeleteMapping("/categories/{categoryId}")
    @Operation(summary = "카테고리 비활성화")
    public void deactivateCategory(@Parameter(hidden = true) CurrentUser currentUser,
            @PathVariable Long categoryId) {
        adminService.deactivateCategory(currentUser.userId(), categoryId);
    }
}
