package com.community.domain.reaction.controller;

import com.community.common.config.security.CustomUserDetails;
import com.community.common.dto.BaseResponse;
import com.community.domain.reaction.dto.ReactionResponse;
import com.community.domain.reaction.enums.ReactionType;
import com.community.domain.reaction.service.ReactionService;
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
            @RequestParam ReactionType type
    ) {
        Long userId = userDetails.getUserId();

        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, reactionService.react(postId, userId, type)));
    }
}
