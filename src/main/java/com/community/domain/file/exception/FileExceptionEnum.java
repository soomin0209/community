package com.community.domain.file.exception;

import com.community.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum FileExceptionEnum implements ErrorCode {
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 파일입니다"),
    FILE_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "파일은 최대 10개까지 업로드 가능합니다"),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다"),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "파일 크기는 10MB를 초과할 수 없습니다"),
    BLOCKED_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "보안상 업로드할 수 없는 파일 형식입니다"),
    INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 형식입니다"),
    FILE_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 파일에 대한 권한이 없습니다"),
    FILE_ALREADY_ATTACHED(HttpStatus.BAD_REQUEST, "이미 다른 게시글에 첨부된 파일입니다");

    private final HttpStatus httpStatus;
    private final String message;

    FileExceptionEnum(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
