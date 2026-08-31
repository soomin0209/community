package com.community.domain.post.dto.response;

import com.community.domain.file.dto.response.FileGetAllResponse;
import com.community.domain.post.enums.PostType;

import java.time.LocalDateTime;
import java.util.List;

public record CreatePostResponse(
        Long id,
        String title,
        String content,
        String nickname,
        PostType type,
        LocalDateTime createdAt,
        List<FileGetAllResponse> files
) {}
