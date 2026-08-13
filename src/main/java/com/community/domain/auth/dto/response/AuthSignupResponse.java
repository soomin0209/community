package com.community.domain.auth.dto.response;

public record AuthSignupResponse(
        Long userId,
        String loginId,
        String nickname
) {}
