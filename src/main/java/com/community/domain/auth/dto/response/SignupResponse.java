package com.community.domain.auth.dto.response;

public record SignupResponse(
        Long id,
        String loginId,
        String nickname
) {}
