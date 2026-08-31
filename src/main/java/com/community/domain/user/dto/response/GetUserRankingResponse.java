package com.community.domain.user.dto.response;

public record GetUserRankingResponse(
        Long id,
        String nickname,
        Long count
) {}
