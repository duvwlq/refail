package com.fail.app.domain.post.repository;

import com.fail.app.domain.post.entity.Post;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.domain.Specification;

public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    @Override
    @EntityGraph(attributePaths = {"category", "user"})
    Page<Post> findAll(Specification<Post> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "user"})
    @Query("select post from Post post where post.id = :postId")
    Optional<Post> findDetailById(@Param("postId") Long postId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Post post set post.reactionCount = post.reactionCount + 1 where post.id = :postId")
    int incrementReactionCount(@Param("postId") Long postId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Post post
            set post.reactionCount = post.reactionCount - 1
            where post.id = :postId and post.reactionCount > 0
            """)
    int decrementReactionCount(@Param("postId") Long postId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Post post set post.reportCount = post.reportCount + 1 where post.id = :postId")
    int incrementReportCount(@Param("postId") Long postId);

    long countByDeletedAtIsNull();
}
