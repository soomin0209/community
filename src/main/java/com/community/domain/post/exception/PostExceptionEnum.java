package com.community.domain.post.exception;

import com.community.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum PostExceptionEnum implements ErrorCode {
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시물입니다");

    private final HttpStatus httpStatus;
    private final String message;

    PostExceptionEnum(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
