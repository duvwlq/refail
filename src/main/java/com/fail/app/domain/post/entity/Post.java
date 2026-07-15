package com.fail.app.domain.post.entity;

import com.fail.app.common.entity.SoftDeleteEntity;
import com.fail.app.domain.category.entity.Category;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostVisibilityType visibilityType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FailureSize failureSize;

    @Column(nullable = false, length = 30)
    private String emotionTag;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdvicePreference advicePreference;

    @Column(nullable = false)
    private boolean retryIntention;

    @Column(length = 500)
    private String nextAttemptPlan;

    @Column(nullable = false)
    private int reactionCount;

    @Column(nullable = false)
    private int reportCount;

    @Column(nullable = false)
    private boolean hidden;

    @Column
    private LocalDateTime hiddenAt;

    @Builder
    private Post(
            User user,
            Category category,
            String title,
            String content,
            PostVisibilityType visibilityType,
            FailureSize failureSize,
            String emotionTag,
            AdvicePreference advicePreference,
            boolean retryIntention,
            String nextAttemptPlan
    ) {
        this.user = user;
        this.category = category;
        this.title = title;
        this.content = content;
        this.visibilityType = visibilityType;
        this.failureSize = failureSize;
        this.emotionTag = emotionTag;
        this.advicePreference = advicePreference;
        this.retryIntention = retryIntention;
        this.nextAttemptPlan = nextAttemptPlan;
    }

    public void hide(LocalDateTime hiddenAt) {
        this.hidden = true;
        this.hiddenAt = hiddenAt;
    }

    public void unhide() {
        this.hidden = false;
        this.hiddenAt = null;
    }

    public void increaseReactionCount() {
        this.reactionCount++;
    }

    public void decreaseReactionCount() {
        if (this.reactionCount > 0) {
            this.reactionCount--;
        }
    }

    public void increaseReportCount() {
        this.reportCount++;
    }

    public void update(
            Category category,
            String title,
            String content,
            FailureSize failureSize,
            String emotionTag,
            AdvicePreference advicePreference,
            boolean retryIntention,
            String nextAttemptPlan
    ) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.failureSize = failureSize;
        this.emotionTag = emotionTag;
        this.advicePreference = advicePreference;
        this.retryIntention = retryIntention;
        this.nextAttemptPlan = nextAttemptPlan;
    }
}
