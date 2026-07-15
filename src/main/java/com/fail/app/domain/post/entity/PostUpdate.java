package com.fail.app.domain.post.entity;

import com.fail.app.common.entity.SoftDeleteEntity;
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
@Table(name = "post_updates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostUpdate extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PostUpdateStatus status;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Builder
    private PostUpdate(Post post, User user, PostUpdateStatus status, String content) {
        this.post = post;
        this.user = user;
        this.status = status;
        this.content = content;
    }

    public void update(PostUpdateStatus status, String content) {
        this.status = status;
        this.content = content;
    }
}
