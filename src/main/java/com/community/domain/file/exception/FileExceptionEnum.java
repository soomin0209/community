package com.community.domain.file.exception;

import com.community.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum FileExceptionEnum implements ErrorCode {
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 파일입니다");

    private final HttpStatus httpStatus;
    private final String message;

    FileExceptionEnum(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
