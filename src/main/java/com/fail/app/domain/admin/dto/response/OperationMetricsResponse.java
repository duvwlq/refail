package com.fail.app.domain.admin.dto.response;

public record OperationMetricsResponse(
        long totalPosts,
        long postsWithUpdates,
        double updateRate,
        long pendingReports,
        long retryingUpdates,
        long pausedUpdates,
        long succeededUpdates
) {
}
