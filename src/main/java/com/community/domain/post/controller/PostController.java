package com.community.domain.post.controller;

import com.community.common.annotation.Idempotent;
import com.community.common.config.security.CustomUserDetails;
import com.community.common.dto.BaseResponse;
import com.community.common.dto.PageResponse;
import com.community.domain.post.dto.request.PostCreateRequest;
import com.community.domain.post.dto.request.PostPageCondition;
import com.community.domain.post.dto.request.PostUpdateRequest;
import com.community.domain.post.dto.response.PostCreateResponse;
import com.community.domain.post.dto.response.PostGetAllResponse;
import com.community.domain.post.dto.response.PostGetOneResponse;
import com.community.domain.post.dto.response.PostUpdateResponse;
import com.community.domain.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    // 게시물 등록
    @Idempotent
    @PostMapping
    public ResponseEntity<BaseResponse<PostCreateResponse>> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PostCreateRequest request
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                HttpStatus.CREATED.name(), null, postService.create(userId, request)));
    }

    // 게시물 단건 조회
    @GetMapping("/{postId}")
    public ResponseEntity<BaseResponse<PostGetOneResponse>> getOne(@PathVariable Long postId) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, postService.getOne(postId)));
    }

    // 게시물 목록 조회
    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<PostGetAllResponse>>> getAll(
            @Valid @ModelAttribute PostPageCondition condition
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, postService.getAll(condition)));
    }

    // 내 게시물 목록 조회
    @GetMapping("/my")
    public ResponseEntity<BaseResponse<PageResponse<PostGetAllResponse>>> getMine(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @ModelAttribute PostPageCondition condition
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, postService.getMine(userId, condition)));
    }

    // 게시물 수정
    @Idempotent
    @PatchMapping("/{postId}")
    public ResponseEntity<BaseResponse<PostUpdateResponse>> update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest request
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
