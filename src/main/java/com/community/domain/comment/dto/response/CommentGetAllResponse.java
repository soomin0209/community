package com.community.domain.comment.dto.response;

import java.time.LocalDateTime;

public record CommentGetAllResponse(
        Long id,
        Long parentId,
        String nickname,
        String content,
        LocalDateTime createdAt,
        int depth
) {}
