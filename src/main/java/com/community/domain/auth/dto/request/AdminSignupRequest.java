package com.community.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminSignupRequest(
        @NotBlank(message = "아이디를 입력해주세요")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[0-9])[a-z0-9]{6,20}$", message = "아이디 형식이 올바르지 않습니다")
        String loginId,

        @NotBlank(message = "닉네임을 입력해주세요")
        @Size(min = 2, max = 16, message = "닉네임은 2~16자여야 합니다")
        String nickname,

        @NotBlank(message = "비밀번호를 입력해주세요")
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[+=%_!@#$^&*?])[a-zA-Z0-9+=%_!@#$^&*?]{8,20}$", message = "비밀번호 형식이 올바르지 않습니다")
        String password,

        @NotBlank(message = "관리자 인증키를 입력해주세요")
        String adminKey
) {}
