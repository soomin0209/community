package com.community.domain.post.dto.response;

public record GetBestPostsResponse(
        Long id,
        String title,
        Long weeklyViewCount,
        Long totalViewCount
) {}
