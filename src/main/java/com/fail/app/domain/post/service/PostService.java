package com.fail.app.domain.post.service;

import com.fail.app.domain.post.dto.request.CreatePostRequest;
import com.fail.app.domain.post.dto.request.CreatePostUpdateRequest;
import com.fail.app.domain.post.dto.request.UpdatePostRequest;
import com.fail.app.domain.post.dto.response.PostDetailResponse;
import com.fail.app.domain.post.dto.response.PostSummaryResponse;
import com.fail.app.domain.post.dto.response.PostUpdateResponse;
import com.fail.app.domain.post.dto.response.PostOwnershipResponse;
import com.fail.app.domain.post.entity.PostSortType;
import com.fail.app.domain.post.entity.FailureSize;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostService {

    PostDetailResponse createPost(Long userId, CreatePostRequest request);

    Page<PostSummaryResponse> getPosts(Long categoryId, FailureSize failureSize, String keyword, PostSortType sortType, Pageable pageable);

    Page<PostSummaryResponse> getMyPosts(Long userId, Pageable pageable);

    PostDetailResponse getPost(Long postId);

    PostOwnershipResponse getOwnership(Long userId, Long postId);

    PostDetailResponse updatePost(Long userId, Long postId, UpdatePostRequest request);

    void deletePost(Long userId, Long postId);

    PostUpdateResponse createUpdate(Long userId, Long postId, CreatePostUpdateRequest request);

    PostUpdateResponse updatePostUpdate(Long userId, Long postId, Long updateId, CreatePostUpdateRequest request);

    void deletePostUpdate(Long userId, Long postId, Long updateId);

    List<PostUpdateResponse> getUpdates(Long postId);
}
