package com.fail.app.domain.admin.service;

import com.fail.app.domain.admin.dto.response.OperationMetricsResponse;
import com.fail.app.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMetricsReader {

    private final PostRepository postRepository;

    public OperationMetricsResponse getMetrics() {
        var metrics = postRepository.getOperationMetrics();
        long total = metrics.getTotalPosts();
        long withUpdates = metrics.getPostsWithUpdates();
        double updateRate = total == 0 ? 0 : Math.round(withUpdates * 1000.0 / total) / 10.0;
        return new OperationMetricsResponse(
                total,
                withUpdates,
                updateRate,
                metrics.getPendingReports(),
                metrics.getRetryingUpdates(),
                metrics.getPausedUpdates(),
                metrics.getSucceededUpdates()
        );
    }
}
