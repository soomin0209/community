package com.community.domain.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentUpdateRequest(
        @NotBlank(message = "수정할 내용이 없습니다")
        @Size(max = 200, message = "댓글은 200자 이하여야 합니다")
        String content
) {}
