package com.fail.app.domain.report.entity;

import com.fail.app.common.entity.BaseTimeEntity;
import com.fail.app.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "reports",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_report_reporter_target",
                columnNames = {"reporter_user_id", "target_type", "target_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_user_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportTargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportReasonType reasonType;

    @Column(columnDefinition = "text")
    private String reasonDetail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Column
    private LocalDateTime processedAt;

    @Column
    private Long processedBy;

    @Builder
    private Report(
            User reporter,
            ReportTargetType targetType,
            Long targetId,
            ReportReasonType reasonType,
            String reasonDetail,
            ReportStatus status
    ) {
        this.reporter = reporter;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reasonType = reasonType;
        this.reasonDetail = reasonDetail;
        this.status = status;
    }

    public void resolve(Long adminUserId, LocalDateTime processedAt) {
        this.status = ReportStatus.RESOLVED;
        this.processedBy = adminUserId;
        this.processedAt = processedAt;
    }
}
