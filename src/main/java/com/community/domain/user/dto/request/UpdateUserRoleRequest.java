package com.community.domain.user.dto.request;

import com.community.domain.user.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull(message = "변경할 회원 등급을 입력해주세요")
        UserRole role
) {}
