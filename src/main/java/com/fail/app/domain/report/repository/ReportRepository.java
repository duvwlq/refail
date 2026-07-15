package com.fail.app.domain.report.repository;

import com.fail.app.domain.report.entity.Report;
import com.fail.app.domain.report.entity.ReportStatus;
import com.fail.app.domain.report.entity.ReportTargetType;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    @Override
    @EntityGraph(attributePaths = "reporter")
    Page<Report> findAll(Pageable pageable);

    Optional<Report> findByReporterIdAndTargetTypeAndTargetId(
            Long reporterId,
            ReportTargetType targetType,
            Long targetId
    );

    @EntityGraph(attributePaths = "reporter")
    Page<Report> findAllByStatus(ReportStatus status, Pageable pageable);

    List<Report> findAllByTargetTypeAndTargetIdAndStatus(
            ReportTargetType targetType,
            Long targetId,
            ReportStatus status
    );

    long countByStatus(ReportStatus status);

    long countByTargetTypeAndTargetId(ReportTargetType targetType, Long targetId);
}
