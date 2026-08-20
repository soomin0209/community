package com.community.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateNicknameRequest(
        @NotBlank(message = "변경할 닉네임을 입력해주세요")
        @Size(min = 2, max = 16, message = "닉네임은 2~16자여야 합니다")
        String nickname
) {}
