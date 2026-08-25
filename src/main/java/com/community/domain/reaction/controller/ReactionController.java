package com.community.domain.reaction.controller;

import com.community.common.config.security.CustomUserDetails;
import com.community.common.dto.BaseResponse;
import com.community.domain.reaction.dto.request.ReactionRequest;
import com.community.domain.reaction.dto.response.ReactionResponse;
import com.community.domain.reaction.service.ReactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/{postId}/reactions")
public class ReactionController {

    private final ReactionService reactionService;

    @PostMapping
    public ResponseEntity<BaseResponse<ReactionResponse>> react(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId,
            @Valid @RequestBody ReactionRequest request
    ) {
        Long userId = userDetails.getUserId();

        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, reactionService.react(postId, userId, request)));
    }
}
