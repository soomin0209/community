package com.community.domain.post.dto.response;

import java.time.LocalDateTime;

public record PostUpdateResponse(
        Long postId,
        String title,
        String content,
        String nickname,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
