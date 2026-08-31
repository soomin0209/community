package com.community.domain.comment.dto.response;

import java.time.LocalDateTime;

public record UpdateCommentResponse(
        Long id,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
