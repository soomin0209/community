package com.community.domain.post.dto.response;

import com.community.domain.post.enums.PostType;

import java.time.LocalDateTime;

public record PostPinResponse(
        Long id,
        String title,
        String nickname,
        PostType type,
        LocalDateTime createdAt,
        Boolean isPinned,
        LocalDateTime pinnedAt
) {}
