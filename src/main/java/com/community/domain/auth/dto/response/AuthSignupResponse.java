package com.community.domain.auth.dto.response;

public record AuthSignupResponse(
        Long id,
        String loginId,
        String nickname
) {}
