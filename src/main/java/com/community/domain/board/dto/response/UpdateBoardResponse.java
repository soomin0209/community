package com.community.domain.board.dto.response;

import java.time.LocalDateTime;

public record UpdateBoardResponse(
        Long id,
        String name,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
