package com.community.domain.auth.exception;

import com.community.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum AuthExceptionEnum implements ErrorCode {
    DUPLICATED_ID(HttpStatus.CONFLICT, "이미 사용중인 아이디입니다"),
    DUPLICATED_NICKNAME(HttpStatus.CONFLICT, "이미 사용중인 닉네임입니다"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다"),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 없습니다"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다"),
    INVALID_ADMIN_KEY(HttpStatus.FORBIDDEN, "관리자 인증키가 유효하지 않습니다");

    private final HttpStatus httpStatus;
    private final String message;

    AuthExceptionEnum(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
