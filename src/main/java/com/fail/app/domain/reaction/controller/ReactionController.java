package com.fail.app.domain.reaction.controller;

import com.fail.app.common.security.CurrentUser;
import com.fail.app.domain.reaction.dto.request.UpsertReactionRequest;
import com.fail.app.domain.reaction.dto.response.ReactionResponse;
import com.fail.app.domain.reaction.service.ReactionService;
import com.fail.app.common.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
import com.fail.app.domain.reaction.dto.response.ReactionCountResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "공감", description = "게시글 공감 등록, 변경, 취소")
@SecurityRequirement(name = OpenApiConfig.JWT_SCHEME_NAME)
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts/{postId}/reaction")
public class ReactionController {

    private final ReactionService reactionService;

    @GetMapping
    @Operation(summary = "내 공감 상태 조회")
    public ReactionResponse getReaction(
            @Parameter(hidden = true) CurrentUser currentUser,
            @PathVariable Long postId
    ) {
        return reactionService.getReaction(currentUser.userId(), postId);
    }

    @GetMapping("/summary")
    @Operation(summary = "공감 유형별 집계 조회")
    public List<ReactionCountResponse> getReactionCounts(@PathVariable Long postId) {
        return reactionService.getReactionCounts(postId);
    }

    @PutMapping
    @Operation(summary = "공감 등록 또는 변경")
    public ReactionResponse upsertReaction(
            @Parameter(hidden = true) CurrentUser currentUser,
            @PathVariable Long postId,
            @Valid @RequestBody UpsertReactionRequest request
    ) {
        return reactionService.upsertReaction(currentUser.userId(), postId, request);
    }

    @DeleteMapping
    @Operation(summary = "공감 취소")
    public ReactionResponse removeReaction(
            @Parameter(hidden = true) CurrentUser currentUser,
            @PathVariable Long postId
    ) {
        return reactionService.removeReaction(currentUser.userId(), postId);
    }
}
