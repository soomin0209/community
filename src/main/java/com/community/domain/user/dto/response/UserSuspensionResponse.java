package com.community.domain.user.dto.response;

import java.time.LocalDateTime;

public record UserSuspensionResponse(
        Long id,
        String reason,
        int day,
        LocalDateTime suspendedAt
) {}
