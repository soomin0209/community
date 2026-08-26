package com.community.domain.user.dto.response;

import java.time.LocalDateTime;

public record UserGetOneResponse(
        Long userId,
        String nickname,
        LocalDateTime createdAt,
        Long visitCount,
        Long postCount,
        Long commentCount
) {}
