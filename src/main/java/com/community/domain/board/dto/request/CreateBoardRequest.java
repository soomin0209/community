package com.community.domain.board.dto.request;

import com.community.domain.user.enums.UserGrade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBoardRequest(
        @NotBlank(message = "게시판 이름을 입력해주세요")
        @Size(max = 20, message = "게시판 이름은 20자 이하여야 합니다")
        String name,

        UserGrade minGrade
) {}
