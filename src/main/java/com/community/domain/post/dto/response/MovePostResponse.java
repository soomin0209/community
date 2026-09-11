package com.community.domain.post.dto.response;

import java.time.LocalDateTime;

public record MovePostResponse(
        Long id,
        Long boardId,
        String title,
        String nickname,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
