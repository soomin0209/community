package com.community.domain.comment.exception;

import com.community.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CommentExceptionEnum implements ErrorCode {
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다"),
    COMMENT_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 댓글의 작성자가 아닙니다"),
    COMMENT_INVALID_PARENT(HttpStatus.BAD_REQUEST, "해당 게시물의 댓글이 아닙니다"),
    COMMENT_DEPTH_LIMIT_EXCEED(HttpStatus.BAD_REQUEST, "댓글 깊이 제한을 초과했습니다");

    private final HttpStatus httpStatus;
    private final String message;

    CommentExceptionEnum(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
