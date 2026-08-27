package com.community.domain.comment.dto.response;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
public class CommentGetAllResponse {
    private final Long id;
    private final Long parentId;
    private final String nickname;
    private final String content;
    private final LocalDateTime createdAt;
    private final int depth;
    private final List<CommentGetAllResponse> children;

    public CommentGetAllResponse(Long id, Long parentId, String nickname, String content, LocalDateTime createdAt, int depth) {
        this.id = id;
        this.parentId = parentId;
        this.nickname = nickname;
        this.content = content;
        this.createdAt = createdAt;
        this.depth = depth;
        this.children = new ArrayList<>();
    }

    public void addChild(CommentGetAllResponse child) {
        this.children.add(child);
    }
}
