package com.community.domain.user.dto.projection;

import java.time.LocalDateTime;

public record UserSuspensionProjection(
        Long id,
        String reason,
        Integer day,
        LocalDateTime suspendedAt
) {}
