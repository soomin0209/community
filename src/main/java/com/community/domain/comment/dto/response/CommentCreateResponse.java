package com.community.domain.comment.dto.response;

import java.time.LocalDateTime;

public record CommentCreateResponse(
        Long id,
        String content,
        LocalDateTime createdAt
) {}
