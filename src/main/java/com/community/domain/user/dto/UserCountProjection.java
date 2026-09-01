package com.community.domain.user.dto;

public record UserCountProjection(
        Long userId,
        Long postCount,
        Long commentCount
) {}
