package com.community.domain.comment.dto.response;

import java.time.LocalDateTime;

public record CommentGetMineResponse(
        Long id,
        Long postId,
        String postTitle,
        String content,
        LocalDateTime createdAt
) {}
