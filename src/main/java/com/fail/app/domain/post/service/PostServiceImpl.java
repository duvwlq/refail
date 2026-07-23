package com.fail.app.domain.post.service;

import com.fail.app.common.exception.ApiException;
import com.fail.app.common.exception.ErrorCode;
import com.fail.app.domain.category.entity.Category;
import com.fail.app.domain.category.repository.CategoryRepository;
import com.fail.app.domain.post.dto.request.CreatePostRequest;
import com.fail.app.domain.post.dto.request.CreatePostUpdateRequest;
import com.fail.app.domain.post.dto.request.UpdatePostRequest;
import com.fail.app.domain.post.dto.response.PostDetailResponse;
import com.fail.app.domain.post.dto.response.PostOwnershipResponse;
import com.fail.app.domain.post.dto.response.PostSummaryResponse;
import com.fail.app.domain.post.dto.response.PostUpdateResponse;
import com.fail.app.domain.post.entity.FailureSize;
import com.fail.app.domain.post.entity.Post;
import com.fail.app.domain.post.entity.PostSortType;
import com.fail.app.domain.post.entity.PostUpdate;
import com.fail.app.domain.post.repository.PostRepository;
import com.fail.app.domain.post.repository.PostUpdateRepository;
import com.fail.app.domain.user.entity.User;
import com.fail.app.domain.user.policy.UserAccessPolicy;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostUpdateRepository postUpdateRepository;
    private final UserAccessPolicy userAccessPolicy;
    private final CategoryRepository categoryRepository;
    private final PostQueryService postQueryService;
    private final Clock clock;

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
        return postQueryService.getPosts(categoryId, failureSize, keyword, sortType, pageable);
    }

    @Override
    public Page<PostSummaryResponse> getMyPosts(Long userId, Pageable pageable) {
        return postQueryService.getMyPosts(userId, pageable);
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
        post.softDelete(LocalDateTime.now(clock));
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
        getPostUpdate(postId, updateId).softDelete(LocalDateTime.now(clock));
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
