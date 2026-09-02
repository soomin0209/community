package com.community.domain.post.dto.response;

import java.time.LocalDateTime;

public record UpdatePostResponse(
        Long id,
        Long boardId,
        String title,
        String content,
        String nickname,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
