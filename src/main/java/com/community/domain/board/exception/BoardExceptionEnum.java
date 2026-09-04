package com.community.domain.board.exception;

import com.community.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BoardExceptionEnum implements ErrorCode {
    DUPLICATED_NAME(HttpStatus.CONFLICT, "이미 존재하는 게시판입니다"),
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시판입니다"),
    BOARD_IN_USE(HttpStatus.CONFLICT, "해당 게시판에 게시물이 존재합니다"),
    BOARD_UPDATE_NO_CONTENT(HttpStatus.BAD_REQUEST, "수정할 내용이 없습니다"),
    BOARD_ACCESS_DENIED(HttpStatus.FORBIDDEN, "게시판 접근 권한이 없습니다");

    private final HttpStatus httpStatus;
    private final String message;

    BoardExceptionEnum(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
