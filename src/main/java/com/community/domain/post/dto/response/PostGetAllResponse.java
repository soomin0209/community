package com.community.domain.post.dto.response;

import java.time.LocalDateTime;

public record PostGetAllResponse(
        Long postId,
        String title,
        String nickname,
        LocalDateTime createdAt
) {}
