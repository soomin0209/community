package com.community.domain.post.controller;

import com.community.common.config.security.CustomUserDetails;
import com.community.common.dto.BaseResponse;
import com.community.domain.post.dto.request.MovePostRequest;
import com.community.domain.post.dto.response.MovePostResponse;
import com.community.domain.post.dto.response.PinPostResponse;
import com.community.domain.post.service.PostManagerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/posts")
public class PostManagerController {

    private final PostManagerService postManagerService;

    // 게시물 고정/해제
    @PatchMapping("/{postId}/pin")
    public ResponseEntity<BaseResponse<PinPostResponse>> pin(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, postManagerService.pin(userId, postId)));
    }

    // 게시물 강제 이동
    @PatchMapping("/{postId}/move")
    public ResponseEntity<BaseResponse<MovePostResponse>> move(
            @PathVariable Long postId,
            @Valid @RequestBody MovePostRequest request
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, postManagerService.move(postId, request)));
    }

    // 게시물 강제 삭제
    @DeleteMapping("/{postId}")
    public ResponseEntity<BaseResponse<Void>> delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId
    ) {
        Long userId = userDetails.getUserId();
        postManagerService.delete(userId, postId);
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, null));
    }
}
