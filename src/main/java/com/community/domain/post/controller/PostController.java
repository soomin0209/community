package com.community.domain.post.controller;

import com.community.common.annotation.Idempotent;
import com.community.common.config.security.CustomUserDetails;
import com.community.common.dto.BaseResponse;
import com.community.common.dto.PageResponse;
import com.community.domain.post.dto.request.CreatePostRequest;
import com.community.domain.post.dto.request.PostPageCondition;
import com.community.domain.post.dto.request.UpdatePostRequest;
import com.community.domain.post.dto.response.*;
import com.community.domain.post.service.PostService;
import com.community.domain.post.service.PostViewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final PostViewService postViewService;

    // 게시물 등록
    @Idempotent
    @PostMapping
    public ResponseEntity<BaseResponse<CreatePostResponse>> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreatePostRequest request
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                HttpStatus.CREATED.name(), null, postService.create(userId, request)));
    }

    // 게시물 단건 조회
    @GetMapping("/{postId}")
    public ResponseEntity<BaseResponse<GetOnePostResponse>> getOne(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId,
            HttpServletRequest request
    ) {
        Long userId = userDetails != null ? userDetails.getUserId() : null;

        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("Proxy-Client-IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("WL-Proxy-Client-IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("HTTP_CLIENT_IP");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getRemoteAddr();
        }

        if (clientIp != null && clientIp.contains(",")) {
            clientIp = clientIp.split(",")[0].trim();
        }

        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, postService.getOne(postId, clientIp, userId)));
    }

    // 게시물 목록 조회
    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<GetAllPostsResponse>>> getAll(
            @Valid @ModelAttribute PostPageCondition condition
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, postService.getAll(condition)));
    }

    // 내 게시물 목록 조회
    @GetMapping("/my")
    public ResponseEntity<BaseResponse<PageResponse<GetAllPostsResponse>>> getMine(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute PostPageCondition condition
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, postService.getMine(userId, condition)));
    }

    // 주간 인기 게시물 목록 조회 (Top 5)
    @GetMapping("/best")
    public ResponseEntity<BaseResponse<List<GetBestPostsResponse>>> getBest() {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, postViewService.getWeeklyBestPosts()));
    }

    // 게시물 수정
    @Idempotent
    @PatchMapping("/{postId}")
    public ResponseEntity<BaseResponse<UpdatePostResponse>> update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId,
            @Valid @RequestBody UpdatePostRequest request
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, postService.update(userId, postId, request)));
    }

    // 게시물 삭제
    @DeleteMapping("/{postId}")
    public ResponseEntity<BaseResponse<Void>> delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId
    ) {
        Long userId = userDetails.getUserId();
        postService.delete(userId, postId);
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, null));
    }
}
