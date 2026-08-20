package com.community.domain.user.exception;

import com.community.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum UserExceptionEnum implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다"),
    NICKNAME_UNCHANGED(HttpStatus.BAD_REQUEST, "기존 닉네임과 동일합니다");

    private final HttpStatus httpStatus;
    private final String message;

    UserExceptionEnum(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
