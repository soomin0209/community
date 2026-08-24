package com.community.domain.post.dto.response;

public record PostGetBest5Response(
        Long id,
        String title,
        Long weeklyViewCount,
        Long totalViewCount
) {}
