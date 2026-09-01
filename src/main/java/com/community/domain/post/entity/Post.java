package com.community.domain.post.entity;

import com.community.common.entity.BaseEntity;
import com.community.common.exception.ServiceErrorException;
import com.community.domain.post.dto.request.UpdatePostRequest;
import com.community.domain.post.enums.PostType;
import com.community.domain.post.exception.PostExceptionEnum;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long boardId;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostType type = PostType.GENERAL;

    @Column(nullable = false)
    private Long viewCount = 0L;

    @Column(nullable = false)
    private Boolean isPinned = false;

    private LocalDateTime pinnedAt;

    public static Post register(
            Long userId,
            Long boardId,
            String title,
            String content,
            PostType type
    ) {
        Post post = new Post();

        post.userId = userId;
        post.boardId = boardId;
        post.title = title;
        post.content = content;
        if (type != null) {
            post.type = type;
        }

        return post;
    }

    public void update(UpdatePostRequest request) {
        if (request.title() == null && request.content() == null) {
            throw new ServiceErrorException(PostExceptionEnum.POST_UPDATE_NO_CONTENT);
        }
        if (request.title() != null) this.title = request.title();
        if (request.content() != null) this.content = request.content();
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void pin() {
        this.isPinned = true;
        this.pinnedAt = LocalDateTime.now();
    }

    public void unpin() {
        this.isPinned = false;
        this.pinnedAt = null;
    }
}
