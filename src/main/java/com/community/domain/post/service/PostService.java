package com.community.domain.post.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.post.dto.request.PostCreateRequest;
import com.community.domain.post.dto.response.PostCreateResponse;
import com.community.domain.post.entity.Post;
import com.community.domain.post.repository.PostRepository;
import com.community.domain.user.entity.User;
import com.community.domain.user.exception.UserExceptionEnum;
import com.community.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostCreateResponse create(Long userId, PostCreateRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        Post post = Post.register(user.getId(), request.title(), request.content());
        postRepository.save(post);

        return new PostCreateResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                user.getNickname(),
                post.getCreatedAt()
        );
    }
}
