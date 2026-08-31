package com.community.domain.user.dto.response;

import java.time.LocalDateTime;

public record UpdateUserNicknameResponse(
        Long id,
        String nickname,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
