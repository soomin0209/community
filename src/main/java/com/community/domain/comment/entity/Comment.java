package com.community.domain.comment.entity;

import com.community.common.entity.BaseEntity;
import com.community.domain.comment.dto.request.UpdateCommentRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "comments", indexes = {
        @Index(name = "idx_comment_deleted_at", columnList = "deletedAt"),
        @Index(name = "idx_comment_post_id", columnList = "postId")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long parentId;

    @Column(nullable = false)
    private Long postId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 200)
    private String content;

    @Column(nullable = false)
    private int depth = 0;

    public static Comment register(
            Long parentId,
            Long postId,
            Long userId,
            String content,
            int depth
    ) {
        Comment comment = new Comment();

        comment.parentId = parentId;
        comment.postId = postId;
        comment.userId = userId;
        comment.content = content;
        comment.depth = depth;

        return comment;
    }

    public void update(UpdateCommentRequest request) {
        this.content = request.content();
    }
}
