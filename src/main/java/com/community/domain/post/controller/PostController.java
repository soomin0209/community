package com.community.domain.post.controller;

import com.community.common.config.security.CustomUserDetails;
import com.community.common.dto.BaseResponse;
import com.community.domain.post.dto.request.PostCreateRequest;
import com.community.domain.post.dto.response.PostCreateResponse;
import com.community.domain.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    // 게시물 등록
    @PostMapping
    public ResponseEntity<BaseResponse<PostCreateResponse>> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PostCreateRequest request
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                HttpStatus.CREATED.name(), "게시물이 등록되었습니다", postService.create(userId, request)));
    }
}
