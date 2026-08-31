package com.community.domain.comment.dto.response;

import java.time.LocalDateTime;

public record CreateCommentResponse(
        Long id,
        Long parentId,
        String content,
        LocalDateTime createdAt
) {}
