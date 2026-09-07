package com.community.domain.user.dto.request;

import com.community.domain.user.enums.UserRole;

public record UpdateUserRoleRequest(
        Long userId,
        UserRole role
) {}
