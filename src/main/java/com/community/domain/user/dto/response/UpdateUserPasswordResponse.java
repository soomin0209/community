package com.community.domain.user.dto.response;

import java.time.LocalDateTime;

public record UpdateUserPasswordResponse(
        Long id,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
