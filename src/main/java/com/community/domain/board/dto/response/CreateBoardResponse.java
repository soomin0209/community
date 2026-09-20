package com.community.domain.board.dto.response;

import com.community.domain.user.enums.UserRole;

import java.time.LocalDateTime;

public record CreateBoardResponse(
        Long id,
        String name,
        UserRole minRole,
        LocalDateTime createdAt
) {}
