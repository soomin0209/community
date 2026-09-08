package com.community.domain.user.dto.response;

import java.time.LocalDateTime;

public record SuspendUserResponse(
        Long userId,
        LocalDateTime suspendedAt,
        String suspendedReason,
        int suspensionDay,
        LocalDateTime suspendedUntil
) {}
