package com.community.domain.comment.controller;

import com.community.common.config.security.CustomUserDetails;
import com.community.common.dto.BaseResponse;
import com.community.common.dto.PageResponse;
import com.community.domain.comment.dto.request.CommentCreateRequest;
import com.community.domain.comment.dto.request.CommentPageCondition;
import com.community.domain.comment.dto.response.CommentCreateResponse;
import com.community.domain.comment.dto.response.CommentGetAllResponse;
import com.community.domain.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<BaseResponse<CommentCreateResponse>> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                HttpStatus.CREATED.name(), null, commentService.create(postId, userId, request)));
    }

    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<CommentGetAllResponse>>> getAll(
            @PathVariable Long postId,
            @Valid @ModelAttribute CommentPageCondition condition
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, commentService.getAll(postId, condition)));
    }
}
