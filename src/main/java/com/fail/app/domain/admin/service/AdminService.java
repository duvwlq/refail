package com.fail.app.domain.admin.service;

import com.fail.app.domain.admin.dto.request.AdminActionRequest;
import com.fail.app.domain.admin.dto.response.AdminActionResponse;
import com.fail.app.domain.admin.dto.response.AdminReportResponse;
import com.fail.app.domain.report.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.fail.app.domain.admin.dto.request.AdminCategoryRequest;
import com.fail.app.domain.category.dto.response.CategoryResponse;
import com.fail.app.domain.admin.dto.response.OperationMetricsResponse;

public interface AdminService {

    Page<AdminReportResponse> getReports(Long adminUserId, ReportStatus status, Pageable pageable);

    AdminActionResponse hidePost(Long adminUserId, Long postId, AdminActionRequest request);

    AdminActionResponse unhidePost(Long adminUserId, Long postId);

    AdminActionResponse restrictUser(Long adminUserId, Long userId, AdminActionRequest request);

    AdminActionResponse activateUser(Long adminUserId, Long userId);

    CategoryResponse createCategory(Long adminUserId, AdminCategoryRequest request);

    CategoryResponse updateCategory(Long adminUserId, Long categoryId, AdminCategoryRequest request);

    void deactivateCategory(Long adminUserId, Long categoryId);

    OperationMetricsResponse getMetrics(Long adminUserId);
}
