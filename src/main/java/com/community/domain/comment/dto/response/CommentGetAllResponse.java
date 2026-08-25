package com.community.domain.comment.dto.response;

import java.time.LocalDateTime;

public record CommentGetAllResponse(
        Long id,
        String nickname,
        String content,
        LocalDateTime createdAt
) {}
