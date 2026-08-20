package com.community.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserUpdatePasswordRequest(
        @NotBlank(message = "현재 비밀번호를 입력해주세요")
        String oldPassword,

        @NotBlank(message = "새 비밀번호를 입력해주세요")
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[+=%_!@#$^&*?])[a-zA-Z0-9+=%_!@#$^&*?]{8,20}$", message = "비밀번호 형식이 올바르지 않습니다")
        String newPassword
) {}
