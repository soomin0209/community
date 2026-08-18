package com.community.domain.post.entity;

import com.community.common.entity.BaseEntity;
import com.community.common.exception.ServiceErrorException;
import com.community.domain.post.dto.request.PostUpdateRequest;
import com.community.domain.post.exception.PostExceptionEnum;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(nullable = false, length = 50)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    public static Post register(
            Long userId,
            String title,
            String content
    ) {
        Post post = new Post();

        post.userId = userId;
        post.title = title;
        post.content = content;

        return post;
    }

    public void update(PostUpdateRequest request) {
        if (request.title() == null && request.content() == null) {
            throw new ServiceErrorException(PostExceptionEnum.POST_UPDATE_NO_CONTENT);
        }
        if (request.title() != null) this.title = request.title();
        if (request.content() != null) this.content = request.content();
    }
}
