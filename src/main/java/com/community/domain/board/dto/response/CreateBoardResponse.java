package com.community.domain.board.dto.response;

import java.time.LocalDateTime;

public record CreateBoardResponse(
        Long id,
        String name,
        LocalDateTime createdAt
) {}
