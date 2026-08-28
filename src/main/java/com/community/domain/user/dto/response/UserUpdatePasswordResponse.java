package com.community.domain.user.dto.response;

import java.time.LocalDateTime;

public record UserUpdatePasswordResponse(
        Long id,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
