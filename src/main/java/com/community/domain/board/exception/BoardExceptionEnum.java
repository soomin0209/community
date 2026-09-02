package com.community.domain.board.exception;

import com.community.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BoardExceptionEnum implements ErrorCode {
    DUPLICATED_NAME(HttpStatus.CONFLICT, "이미 존재하는 게시판입니다"),
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시판입니다"),
    NAME_UNCHANGED(HttpStatus.BAD_REQUEST, "기존 게시판 이름과 동일합니다"),
    BOARD_IN_USE(HttpStatus.CONFLICT, "해당 게시판에 게시물이 존재합니다");

    private final HttpStatus httpStatus;
    private final String message;

    BoardExceptionEnum(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
