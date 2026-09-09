package com.community.domain.post.dto.request;

import jakarta.validation.constraints.NotNull;

public record MovePostRequest(
        @NotNull(message = "변경할 게시판 아이디를 입력해주세요")
        Long boardId
) {}
