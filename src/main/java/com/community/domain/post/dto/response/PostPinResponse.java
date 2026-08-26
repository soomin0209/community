package com.community.domain.post.dto.response;

import java.time.LocalDateTime;

public record PostPinResponse(
        Long id,
        Boolean isPinned,
        LocalDateTime pinnedAt
) {}
