package com.fail.app.domain.moderation.entity;

import com.fail.app.common.entity.BaseTimeEntity;
import com.fail.app.domain.report.entity.ReportTargetType;
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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "moderation_actions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ModerationAction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_user_id", nullable = false)
    private User adminUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportTargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ModerationActionType actionType;

    @Column(columnDefinition = "text")
    private String reason;

    @Builder
    private ModerationAction(
            User adminUser,
            ReportTargetType targetType,
            Long targetId,
            ModerationActionType actionType,
            String reason
    ) {
        this.adminUser = adminUser;
        this.targetType = targetType;
        this.targetId = targetId;
        this.actionType = actionType;
        this.reason = reason;
    }
}
