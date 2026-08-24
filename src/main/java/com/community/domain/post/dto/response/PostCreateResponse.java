package com.community.domain.post.dto.response;

import com.community.domain.post.enums.PostType;

import java.time.LocalDateTime;

public record PostCreateResponse(
        Long postId,
        String title,
        String content,
        String nickname,
        PostType type,
        LocalDateTime createdAt
) {}
