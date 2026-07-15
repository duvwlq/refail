package com.fail.app.domain.reaction.entity;

import com.fail.app.common.entity.BaseTimeEntity;
import com.fail.app.domain.post.entity.Post;
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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "reactions",
        uniqueConstraints = @UniqueConstraint(name = "uk_reaction_post_user", columnNames = {"post_id", "user_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reaction extends BaseTimeEntity {

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
    private ReactionType reactionType;

    @Builder
    private Reaction(Post post, User user, ReactionType reactionType) {
        this.post = post;
        this.user = user;
        this.reactionType = reactionType;
    }

    public void changeReactionType(ReactionType reactionType) {
        this.reactionType = reactionType;
    }
}
