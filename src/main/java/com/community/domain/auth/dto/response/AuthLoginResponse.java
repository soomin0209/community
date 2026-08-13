package com.community.domain.auth.dto.response;

public record AuthLoginResponse(
        String accessToken,
        String refreshToken
) {}
