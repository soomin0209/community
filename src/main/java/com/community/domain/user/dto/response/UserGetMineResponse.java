package com.community.domain.user.dto.response;

import java.time.LocalDateTime;

public record UserGetMineResponse(
        Long userId,
        String loginId,
        String nickname,
        LocalDateTime createdAt
) {
}
