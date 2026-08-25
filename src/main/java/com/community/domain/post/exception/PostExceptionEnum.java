package com.community.domain.post.exception;

import com.community.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum PostExceptionEnum implements ErrorCode {
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시물입니다"),
    POST_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 게시물의 작성자가 아닙니다"),
    POST_UPDATE_NO_CONTENT(HttpStatus.BAD_REQUEST, "수정할 내용이 없습니다"),
    POST_NOTICE_FORBIDDEN(HttpStatus.FORBIDDEN, "공지글 작성 권한이 없습니다"),
    POST_NOT_NOTICE(HttpStatus.BAD_REQUEST, "공지글만 고정할 수 있습니다");

    private final HttpStatus httpStatus;
    private final String message;

    PostExceptionEnum(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
