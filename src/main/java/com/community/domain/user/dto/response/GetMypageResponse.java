package com.community.domain.user.dto.response;

import com.community.domain.user.enums.UserRole;

import java.time.LocalDateTime;

public record GetMypageResponse(
        Long id,
        String loginId,
        String nickname,
        LocalDateTime createdAt,
        Long visitCount,
        Long postCount,
        Long commentCount,
        UserRole role,
        LocalDateTime suspendedAt,
        String suspendedReason,
        LocalDateTime suspendedUntil
) {}
