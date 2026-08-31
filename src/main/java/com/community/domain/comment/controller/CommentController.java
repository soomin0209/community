package com.community.domain.comment.controller;

import com.community.common.config.security.CustomUserDetails;
import com.community.common.dto.BaseResponse;
import com.community.common.dto.CursorResponse;
import com.community.common.dto.PageResponse;
import com.community.domain.comment.dto.request.CreateCommentRequest;
import com.community.domain.comment.dto.request.CommentCursorCondition;
import com.community.domain.comment.dto.request.CommentPageCondition;
import com.community.domain.comment.dto.request.UpdateCommentRequest;
import com.community.domain.comment.dto.response.CreateCommentResponse;
import com.community.domain.comment.dto.response.GetAllCommentsResponse;
import com.community.domain.comment.dto.response.GetMyCommentsResponse;
import com.community.domain.comment.dto.response.UpdateCommentResponse;
import com.community.domain.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // 댓글 등록
    @PostMapping("/api/posts/{postId}/comments")
    public ResponseEntity<BaseResponse<CreateCommentResponse>> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                HttpStatus.CREATED.name(), null, commentService.create(postId, userId, request)));
    }

    // 댓글 목록 조회
    @GetMapping("/api/posts/{postId}/comments")
    public ResponseEntity<BaseResponse<CursorResponse<GetAllCommentsResponse>>> getAll(
            @PathVariable Long postId,
            @Valid @ModelAttribute CommentCursorCondition condition
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, commentService.getAll(postId, condition)));
    }

    // 내 댓글 목록 조회
    @GetMapping("/api/comments/my")
    public ResponseEntity<BaseResponse<PageResponse<GetMyCommentsResponse>>> getMine(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute CommentPageCondition condition
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, commentService.getMine(userId, condition)));
    }

    // 댓글 수정
    @PatchMapping("/api/comments/{commentId}")
    public ResponseEntity<BaseResponse<UpdateCommentResponse>> update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, commentService.update(userId, commentId, request)));
    }

    // 댓글 삭제
    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<BaseResponse<Void>> delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long commentId
    ) {
        Long userId = userDetails.getUserId();
        commentService.delete(userId, commentId);
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, null));
    }
}
