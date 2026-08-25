package com.community.domain.post.dto.response;

import com.community.domain.post.enums.PostType;

import java.time.LocalDateTime;

public record PostGetAllResponse(
        Long postId,
        String title,
        String nickname,
        PostType type,
        Boolean isPinned,
        LocalDateTime createdAt,
        Long viewCount
) {}
