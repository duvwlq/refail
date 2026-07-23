package com.fail.app.domain.post.service;

import com.fail.app.common.config.SearchProperties;
import com.fail.app.domain.post.dto.response.PostSummaryResponse;
import com.fail.app.domain.post.entity.FailureSize;
import com.fail.app.domain.post.entity.Post;
import com.fail.app.domain.post.entity.PostSortType;
import com.fail.app.domain.post.repository.PostRepository;
import com.fail.app.domain.post.repository.PostUpdateRepository;
import com.fail.app.domain.user.policy.UserAccessPolicy;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryService {

    private final PostRepository postRepository;
    private final PostUpdateRepository postUpdateRepository;
    private final UserAccessPolicy userAccessPolicy;
    private final SearchProperties searchProperties;

    public Page<PostSummaryResponse> getPosts(
            Long categoryId,
            FailureSize failureSize,
            String keyword,
            PostSortType sortType,
            Pageable pageable
    ) {
        if (shouldUseFullText(keyword)) {
            return searchFullText(categoryId, failureSize, keyword.trim(), sortType, pageable);
        }

        Sort sort = sortType == PostSortType.POPULAR
                ? Sort.by(Sort.Order.desc("reactionCount"), Sort.Order.desc("createdAt"))
                : Sort.by(Sort.Order.desc("createdAt"));
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        Specification<Post> visiblePosts = (root, query, builder) -> builder.and(
                builder.isNull(root.get("deletedAt")),
                builder.isFalse(root.get("hidden"))
        );
        if (categoryId != null) {
            visiblePosts = visiblePosts.and(
                    (root, query, builder) -> builder.equal(root.get("category").get("id"), categoryId)
            );
        }
        if (failureSize != null) {
            visiblePosts = visiblePosts.and(
                    (root, query, builder) -> builder.equal(root.get("failureSize"), failureSize)
            );
        }
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            visiblePosts = visiblePosts.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("title")), pattern),
                    builder.like(builder.lower(root.get("content")), pattern),
                    builder.like(builder.lower(root.get("emotionTag")), pattern)
            ));
        }

        Page<Post> posts = postRepository.findAll(visiblePosts, sortedPageable);
        Set<Long> postIdsWithUpdates = findPostIdsWithUpdates(posts.getContent());
        return posts.map(post -> PostSummaryResponse.of(post, postIdsWithUpdates.contains(post.getId())));
    }

    public Page<PostSummaryResponse> getMyPosts(Long userId, Pageable pageable) {
        userAccessPolicy.getActiveUser(userId);
        Specification<Post> mine = (root, query, builder) -> builder.and(
                builder.equal(root.get("user").get("id"), userId),
                builder.isNull(root.get("deletedAt"))
        );
        Page<Post> posts = postRepository.findAll(mine, pageable);
        Set<Long> postIdsWithUpdates = findPostIdsWithUpdates(posts.getContent());
        return posts.map(post -> PostSummaryResponse.of(post, postIdsWithUpdates.contains(post.getId())));
    }

    private Page<PostSummaryResponse> searchFullText(
            Long categoryId,
            FailureSize failureSize,
            String keyword,
            PostSortType sortType,
            Pageable pageable
    ) {
        List<Long> ids = postRepository.searchFullTextIds(
                categoryId,
                failureSize == null ? null : failureSize.name(),
                keyword,
                sortType.name(),
                pageable.getPageSize(),
                pageable.getOffset()
        );
        long total = postRepository.countFullText(
                categoryId,
                failureSize == null ? null : failureSize.name(),
                keyword
        );
        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        Map<Long, Post> postsById = postRepository.findAllByIdIn(ids).stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));
        Set<Long> postIdsWithUpdates = postUpdateRepository.findPostIdsWithUpdates(ids);
        List<PostSummaryResponse> content = ids.stream()
                .map(postsById::get)
                .map(post -> PostSummaryResponse.of(post, postIdsWithUpdates.contains(post.getId())))
                .toList();
        return new PageImpl<>(content, pageable, total);
    }

    private Set<Long> findPostIdsWithUpdates(List<Post> posts) {
        List<Long> postIds = posts.stream().map(Post::getId).toList();
        return postIds.isEmpty() ? Set.of() : postUpdateRepository.findPostIdsWithUpdates(postIds);
    }

    private boolean shouldUseFullText(String keyword) {
        return searchProperties.fulltextEnabled() && keyword != null && keyword.trim().length() >= 2;
    }
}
