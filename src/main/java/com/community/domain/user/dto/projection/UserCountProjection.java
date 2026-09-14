package com.community.domain.user.dto.projection;

public record UserCountProjection(
        Long userId,
        Long postCount,
        Long commentCount
) {}
