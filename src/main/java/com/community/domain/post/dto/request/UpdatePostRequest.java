package com.community.domain.post.dto.request;

import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdatePostRequest(
        @Size(min = 1, max = 50, message = "제목은 1~50자여야 합니다")
        String title,

        @Size(min = 1, message = "내용은 최소 1자 이상이어야 합니다")
        String content,

        List<Long> fileIds
) {}
