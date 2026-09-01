package com.community.domain.board.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateBoardRequest(
        @NotBlank(message = "수정할 내용이 없습니다")
        @Size(max = 20, message = "게시판 이름은 20자 이하여야 합니다")
        String name
) {}
