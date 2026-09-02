package com.community.domain.post.dto.request;

import com.community.domain.post.enums.PostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePostRequest(
        Long boardId,

        @NotBlank(message = "제목을 입력해주세요")
        @Size(max = 50, message = "제목은 50자 이하여야 합니다")
        String title,

        @NotBlank(message = "내용을 입력해주세요")
        String content,

        PostType type,

        List<Long> fileIds
) {}
