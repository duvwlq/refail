package com.fail.app.domain.reaction.repository;

import com.fail.app.domain.reaction.entity.Reaction;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    Optional<Reaction> findByPostIdAndUserId(Long postId, Long userId);

    void deleteByPostIdAndUserId(Long postId, Long userId);

    @Query("select r.reactionType, count(r) from Reaction r where r.post.id = :postId group by r.reactionType")
    List<Object[]> countByTypeForPost(Long postId);

    long countByPostId(Long postId);
}
