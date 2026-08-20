package com.community.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CommonExceptionEnum implements ErrorCode {
    MISSING_IDEMPOTENCY_KEY(HttpStatus.BAD_REQUEST, "Idempotency-Key 헤더가 필요합니다"),
    DUPLICATE_REQUEST(HttpStatus.TOO_MANY_REQUESTS, "동일한 요청이 처리중입니다"),
    REDIS_CONNECTION_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "일시적인 서버 오류로 요청을 처리할 수 없습니다");

    private final HttpStatus httpStatus;
    private final String message;

    CommonExceptionEnum(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
