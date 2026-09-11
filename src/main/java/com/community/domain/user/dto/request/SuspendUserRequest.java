package com.community.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SuspendUserRequest(
        @NotBlank(message = "정지 사유를 입력해주세요")
        String suspendedReason,

        @NotNull(message = "정지 기간을 입력해주세요")
        @Positive(message = "정지 기간은 1일 이상이어야 합니다")
        int suspensionDay
) {}
