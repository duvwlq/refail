package com.fail.app.domain.post.repository;

import com.fail.app.domain.post.entity.PostUpdate;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostUpdateRepository extends JpaRepository<PostUpdate, Long> {

    List<PostUpdate> findAllByPostIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long postId);

    @Query("""
            select distinct postUpdate.post.id
            from PostUpdate postUpdate
            where postUpdate.post.id in :postIds
              and postUpdate.deletedAt is null
            """)
    Set<Long> findPostIdsWithUpdates(@Param("postIds") List<Long> postIds);

    @Query("select count(distinct postUpdate.post.id) from PostUpdate postUpdate where postUpdate.deletedAt is null")
    long countPostsWithUpdates();

    long countByStatusAndDeletedAtIsNull(com.fail.app.domain.post.entity.PostUpdateStatus status);
}
