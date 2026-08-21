package com.community.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthLoginResponse(
        String accessToken,
        String refreshToken
) {}
