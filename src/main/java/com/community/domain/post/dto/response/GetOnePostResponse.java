package com.community.domain.post.dto.response;

import com.community.domain.file.dto.response.GetAllFilesResponse;
import com.community.domain.post.enums.PostType;

import java.time.LocalDateTime;
import java.util.List;

public record GetOnePostResponse(
        Long id,
        String title,
        String content,
        String nickname,
        PostType type,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long viewCount,
        Long likeCount,
        Long dislikeCount,
        List<GetAllFilesResponse> files
) {}
