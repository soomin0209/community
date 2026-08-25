package com.community.domain.comment.dto.response;

import java.time.LocalDateTime;

public record CommentUpdateResponse(
        Long id,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
