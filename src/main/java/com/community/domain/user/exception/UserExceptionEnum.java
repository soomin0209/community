package com.community.domain.user.exception;

import com.community.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum UserExceptionEnum implements ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다"),
    NICKNAME_UNCHANGED(HttpStatus.BAD_REQUEST, "기존 닉네임과 동일합니다"),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다"),
    PASSWORD_UNCHANGED(HttpStatus.BAD_REQUEST, "새 비밀번호가 현재 비밀번호와 동일합니다"),
    USER_MODIFICATION_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 사용자를 변경할 권한이 없습니다"),
    USER_SUSPENDED(HttpStatus.FORBIDDEN, "정지된 사용자는 접근 권한이 없습니다"),
    USER_ALREADY_SUSPENDED(HttpStatus.BAD_REQUEST, "이미 정지된 사용자입니다"),
    USER_NOT_SUSPENDED(HttpStatus.BAD_REQUEST, "정지되지 않은 사용자입니다");

    private final HttpStatus httpStatus;
    private final String message;

    UserExceptionEnum(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
