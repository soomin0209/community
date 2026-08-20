package com.community.domain.user.dto.response;

import java.time.LocalDateTime;

public record UserUpdatePasswordResponse(
        Long userId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
