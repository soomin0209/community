package com.community.domain.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @Positive(message = "부모 식별자는 1 이상이어야 합니다")
        Long parentId,

        @NotBlank(message = "댓글을 입력해주세요")
        @Size(max = 200, message = "댓글은 200자 이하여야 합니다")
        String content
) {}
