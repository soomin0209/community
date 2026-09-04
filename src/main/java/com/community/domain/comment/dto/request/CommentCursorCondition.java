package com.community.domain.comment.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentCursorCondition {
    private Long cursor;

    @Positive(message = "페이지 크기는 1 이상이어야 합니다")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다")
    private int size = 10;

    // TODO 댓글 내용 검색 키워드 추가
}
