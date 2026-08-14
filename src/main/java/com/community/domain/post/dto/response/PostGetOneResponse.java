package com.community.domain.post.dto.response;

import java.time.LocalDateTime;

public record PostGetOneResponse(
        Long postId,
        String title,
        String content,
        String nickname,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
