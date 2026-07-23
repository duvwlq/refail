package com.fail.app.domain.post.service;

import com.fail.app.common.exception.ApiException;
import com.fail.app.common.exception.ErrorCode;
import com.fail.app.domain.category.entity.Category;
import com.fail.app.domain.category.repository.CategoryRepository;
import com.fail.app.domain.post.dto.request.CreatePostRequest;
import com.fail.app.domain.post.dto.request.CreatePostUpdateRequest;
import com.fail.app.domain.post.dto.request.UpdatePostRequest;
import com.fail.app.domain.post.dto.response.PostDetailResponse;
import com.fail.app.domain.post.dto.response.PostSummaryResponse;
import com.fail.app.domain.post.dto.response.PostUpdateResponse;
import com.fail.app.domain.post.dto.response.PostOwnershipResponse;
import com.fail.app.domain.post.entity.Post;
import com.fail.app.domain.post.entity.PostUpdate;
import com.fail.app.domain.post.entity.PostSortType;
import com.fail.app.domain.post.entity.FailureSize;
import com.fail.app.domain.post.repository.PostRepository;
import com.fail.app.domain.post.repository.PostUpdateRepository;
import com.fail.app.domain.user.entity.User;
import com.fail.app.domain.user.policy.UserAccessPolicy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostUpdateRepository postUpdateRepository;
    private final UserAccessPolicy userAccessPolicy;
    private final CategoryRepository categoryRepository;

    @Value("${app.search.fulltext-enabled:false}")
    private boolean fullTextSearchEnabled;

    @Override
    @Transactional
    public PostDetailResponse createPost(Long userId, CreatePostRequest request) {
        User user = getActiveUser(userId);
        Category category = getCategory(request.categoryId());

        Post post = Post.builder()
                .user(user)
                .category(category)
                .title(request.title())
                .content(request.content())
                .visibilityType(request.visibilityType())
                .failureSize(request.failureSize())
                .emotionTag(request.emotionTag())
                .advicePreference(request.advicePreference())
                .retryIntention(request.retryIntention())
                .nextAttemptPlan(request.nextAttemptPlan())
                .build();

        return PostDetailResponse.of(postRepository.save(post), List.of());
    }

    @Override
    public Page<PostSummaryResponse> getPosts(Long categoryId, FailureSize failureSize, String keyword, PostSortType sortType, Pageable pageable) {
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
            visiblePosts = visiblePosts.and((root, query, builder) -> builder.equal(root.get("failureSize"), failureSize));
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
        List<Long> postIds = posts.getContent().stream().map(Post::getId).toList();
        Set<Long> postIdsWithUpdates = postIds.isEmpty()
                ? Set.of()
                : postUpdateRepository.findPostIdsWithUpdates(postIds);

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
        List<Post> orderedPosts = ids.stream().map(postsById::get).toList();
        Set<Long> postIdsWithUpdates = postUpdateRepository.findPostIdsWithUpdates(ids);
        List<PostSummaryResponse> content = orderedPosts.stream()
                .map(post -> PostSummaryResponse.of(post, postIdsWithUpdates.contains(post.getId())))
                .toList();
        return new PageImpl<>(content, pageable, total);
    }

    private boolean shouldUseFullText(String keyword) {
        return fullTextSearchEnabled && keyword != null && keyword.trim().length() >= 2;
    }

    @Override
    public Page<PostSummaryResponse> getMyPosts(Long userId, Pageable pageable) {
        getActiveUser(userId);
        Specification<Post> mine = (root, query, builder) -> builder.and(
                builder.equal(root.get("user").get("id"), userId),
                builder.isNull(root.get("deletedAt"))
        );
        Page<Post> posts = postRepository.findAll(mine, pageable);
        List<Long> ids = posts.getContent().stream().map(Post::getId).toList();
        Set<Long> withUpdates = ids.isEmpty() ? Set.of() : postUpdateRepository.findPostIdsWithUpdates(ids);
        return posts.map(post -> PostSummaryResponse.of(post, withUpdates.contains(post.getId())));
    }

    @Override
    public PostDetailResponse getPost(Long postId) {
        Post post = getPostEntity(postId);
        return PostDetailResponse.of(post, findUpdates(postId));
    }

    @Override
    public PostOwnershipResponse getOwnership(Long userId, Long postId) {
        User user = getActiveUser(userId);
        Post post = getPostEntity(postId);
        return new PostOwnershipResponse(postId, post.getUser().getId().equals(user.getId()));
    }

    @Override
    @Transactional
    public PostDetailResponse updatePost(Long userId, Long postId, UpdatePostRequest request) {
        User user = getActiveUser(userId);
        Post post = getPostEntity(postId);
        validateOwnership(user, post);
        Category category = getCategory(request.categoryId());

        post.update(
                category,
                request.title(),
                request.content(),
                request.failureSize(),
                request.emotionTag(),
                request.advicePreference(),
                request.retryIntention(),
                request.nextAttemptPlan()
        );

        return PostDetailResponse.of(post, findUpdates(postId));
    }

    @Override
    @Transactional
    public void deletePost(Long userId, Long postId) {
        User user = getActiveUser(userId);
        Post post = getPostEntity(postId);
        validateOwnership(user, post);
        post.softDelete(LocalDateTime.now());
    }

    @Override
    @Transactional
    public PostUpdateResponse createUpdate(Long userId, Long postId, CreatePostUpdateRequest request) {
        User user = getActiveUser(userId);
        Post post = getPostEntity(postId);
        validateOwnership(user, post);

        PostUpdate postUpdate = PostUpdate.builder()
                .post(post)
                .user(user)
                .status(request.status())
                .content(request.content())
                .build();

        return PostUpdateResponse.from(postUpdateRepository.save(postUpdate));
    }

    @Override
    @Transactional
    public PostUpdateResponse updatePostUpdate(Long userId, Long postId, Long updateId, CreatePostUpdateRequest request) {
        User user = getActiveUser(userId);
        Post post = getPostEntity(postId);
        validateOwnership(user, post);
        PostUpdate update = getPostUpdate(postId, updateId);
        update.update(request.status(), request.content());
        return PostUpdateResponse.from(update);
    }

    @Override
    @Transactional
    public void deletePostUpdate(Long userId, Long postId, Long updateId) {
        User user = getActiveUser(userId);
        Post post = getPostEntity(postId);
        validateOwnership(user, post);
        getPostUpdate(postId, updateId).softDelete(LocalDateTime.now());
    }

    @Override
    public List<PostUpdateResponse> getUpdates(Long postId) {
        getPostEntity(postId);
        return findUpdates(postId);
    }

    private List<PostUpdateResponse> findUpdates(Long postId) {
        return postUpdateRepository.findAllByPostIdAndDeletedAtIsNullOrderByCreatedAtAsc(postId)
                .stream()
                .map(PostUpdateResponse::from)
                .toList();
    }

    private User getActiveUser(Long userId) {
        return userAccessPolicy.getActiveUser(userId);
    }

    private Category getCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND));
        if (!category.isActive()) {
            throw new ApiException(ErrorCode.CATEGORY_INACTIVE);
        }
        return category;
    }

    private Post getPostEntity(Long postId) {
        Post post = postRepository.findDetailById(postId)
                .orElseThrow(() -> new ApiException(ErrorCode.POST_NOT_FOUND));
        if (post.isDeleted() || post.isHidden()) {
            throw new ApiException(ErrorCode.POST_NOT_FOUND);
        }
        return post;
    }

    private void validateOwnership(User user, Post post) {
        if (!post.getUser().getId().equals(user.getId())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
    }

    private PostUpdate getPostUpdate(Long postId, Long updateId) {
        PostUpdate update = postUpdateRepository.findById(updateId)
                .orElseThrow(() -> new ApiException(ErrorCode.POST_NOT_FOUND));
        if (update.isDeleted() || !update.getPost().getId().equals(postId)) {
            throw new ApiException(ErrorCode.POST_NOT_FOUND);
        }
        return update;
    }
}
