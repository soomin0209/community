package com.community.domain.category.exception;

import com.community.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CategoryExceptionEnum implements ErrorCode {
    DUPLICATED_NAME(HttpStatus.CONFLICT, "이미 존재하는 카테고리입니다"),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 카테고리입니다"),
    NAME_UNCHANGED(HttpStatus.BAD_REQUEST, "기존 카테고리명과 동일합니다");

    private final HttpStatus httpStatus;
    private final String message;

    CategoryExceptionEnum(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
