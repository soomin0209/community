package com.community.domain.user.dto.response;

import java.time.LocalDateTime;

public record UserGetMineResponse(
        Long id,
        String loginId,
        String nickname,
        LocalDateTime createdAt,
        Long visitCount,
        Long postCount,
        Long commentCount
) {}
