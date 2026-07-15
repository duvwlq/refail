package com.fail.app.domain.post.controller;

import com.fail.app.common.security.CurrentUser;
import com.fail.app.domain.post.dto.request.CreatePostRequest;
import com.fail.app.domain.post.dto.request.CreatePostUpdateRequest;
import com.fail.app.domain.post.dto.response.PostDetailResponse;
import com.fail.app.domain.post.dto.response.PostSummaryResponse;
import com.fail.app.domain.post.dto.response.PostUpdateResponse;
import com.fail.app.domain.post.dto.response.PostOwnershipResponse;
import com.fail.app.domain.post.dto.response.DeletePostResponse;
import com.fail.app.domain.post.dto.response.DeletePostUpdateResponse;
import com.fail.app.domain.post.dto.request.UpdatePostRequest;
import com.fail.app.domain.post.service.PostService;
import com.fail.app.common.web.ListQueryPolicy;
import com.fail.app.domain.post.entity.PostSortType;
import com.fail.app.domain.post.entity.FailureSize;
import com.fail.app.common.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@Tag(name = "게시글", description = "실패 게시글과 후속 기록 관리")
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;

    @PostMapping
    @Operation(summary = "게시글 작성")
    @SecurityRequirement(name = OpenApiConfig.JWT_SCHEME_NAME)
    public PostDetailResponse createPost(
            @Parameter(hidden = true) CurrentUser currentUser,
            @Valid @RequestBody CreatePostRequest request
    ) {
        return postService.createPost(currentUser.userId(), request);
    }

    @GetMapping
    @Operation(summary = "게시글 목록 조회", description = "최신순 또는 인기순으로 조회하며 카테고리 필터를 지원합니다.")
    public Page<PostSummaryResponse> getPosts(
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) FailureSize failureSize,
            @Parameter(schema = @Schema(maxLength = ListQueryPolicy.POST_KEYWORD_MAX_LENGTH))
            @RequestParam(required = false) String keyword,
            @Parameter(schema = @Schema(minimum = "0", defaultValue = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(schema = @Schema(minimum = "1", maximum = "50", defaultValue = "20"))
            @RequestParam(defaultValue = "20") int size
    ) {
        return postService.getPosts(
                categoryId,
                failureSize,
                ListQueryPolicy.normalizeKeyword(keyword, ListQueryPolicy.POST_KEYWORD_MAX_LENGTH),
                PostSortType.from(sort),
                ListQueryPolicy.pageRequest(page, size)
        );
    }

    @GetMapping("/me")
    @Operation(summary = "내 게시글 목록 조회")
    @SecurityRequirement(name = OpenApiConfig.JWT_SCHEME_NAME)
    public Page<PostSummaryResponse> getMyPosts(
            @Parameter(hidden = true) CurrentUser currentUser,
            @Parameter(schema = @Schema(minimum = "0", defaultValue = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(schema = @Schema(minimum = "1", maximum = "50", defaultValue = "20"))
            @RequestParam(defaultValue = "20") int size
    ) {
        return postService.getMyPosts(
                currentUser.userId(),
                ListQueryPolicy.pageRequest(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
    }

    @GetMapping("/{postId}")
    @Operation(summary = "게시글 상세 조회")
    public PostDetailResponse getPost(@PathVariable Long postId) {
        return postService.getPost(postId);
    }

    @GetMapping("/{postId}/ownership")
    @Operation(summary = "내 게시글 여부 조회")
    @SecurityRequirement(name = OpenApiConfig.JWT_SCHEME_NAME)
    public PostOwnershipResponse getOwnership(
            @Parameter(hidden = true) CurrentUser currentUser,
            @PathVariable Long postId
    ) {
        return postService.getOwnership(currentUser.userId(), postId);
    }

    @PatchMapping("/{postId}")
    @Operation(summary = "게시글 수정")
    @SecurityRequirement(name = OpenApiConfig.JWT_SCHEME_NAME)
    public PostDetailResponse updatePost(
            @Parameter(hidden = true) CurrentUser currentUser,
            @PathVariable Long postId,
            @Valid @RequestBody UpdatePostRequest request
    ) {
        return postService.updatePost(currentUser.userId(), postId, request);
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "게시글 삭제")
    @SecurityRequirement(name = OpenApiConfig.JWT_SCHEME_NAME)
    public DeletePostResponse deletePost(
            @Parameter(hidden = true) CurrentUser currentUser,
            @PathVariable Long postId
    ) {
        postService.deletePost(currentUser.userId(), postId);
        return new DeletePostResponse(postId, true);
    }

    @PostMapping("/{postId}/updates")
    @Operation(summary = "후속 기록 작성")
    @SecurityRequirement(name = OpenApiConfig.JWT_SCHEME_NAME)
    public PostUpdateResponse createUpdate(
            @Parameter(hidden = true) CurrentUser currentUser,
            @PathVariable Long postId,
            @Valid @RequestBody CreatePostUpdateRequest request
    ) {
        return postService.createUpdate(currentUser.userId(), postId, request);
    }

    @PatchMapping("/{postId}/updates/{updateId}")
    @Operation(summary = "후속 기록 수정")
    @SecurityRequirement(name = OpenApiConfig.JWT_SCHEME_NAME)
    public PostUpdateResponse updatePostUpdate(
            @Parameter(hidden = true) CurrentUser currentUser,
            @PathVariable Long postId,
            @PathVariable Long updateId,
            @Valid @RequestBody CreatePostUpdateRequest request
    ) {
        return postService.updatePostUpdate(currentUser.userId(), postId, updateId, request);
    }

    @DeleteMapping("/{postId}/updates/{updateId}")
    @Operation(summary = "후속 기록 삭제")
    @SecurityRequirement(name = OpenApiConfig.JWT_SCHEME_NAME)
    public DeletePostUpdateResponse deletePostUpdate(
            @Parameter(hidden = true) CurrentUser currentUser,
            @PathVariable Long postId,
            @PathVariable Long updateId
    ) {
        postService.deletePostUpdate(currentUser.userId(), postId, updateId);
        return new DeletePostUpdateResponse(updateId, true);
    }

    @GetMapping("/{postId}/updates")
    @Operation(summary = "후속 기록 목록 조회")
    public List<PostUpdateResponse> getUpdates(@PathVariable Long postId) {
        return postService.getUpdates(postId);
    }
}
