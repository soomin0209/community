package com.community.domain.user.dto.response;

import com.community.domain.user.enums.UserRole;

import java.time.LocalDateTime;

public record UpdateUserRoleResponse(
        Long userId,
        UserRole role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
