package com.community.domain.post.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.post.dto.response.PinPostResponse;
import com.community.domain.post.entity.Post;
import com.community.domain.post.exception.PostExceptionEnum;
import com.community.domain.post.repository.PostRepository;
import com.community.domain.user.entity.User;
import com.community.domain.user.exception.UserExceptionEnum;
import com.community.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PostAdminService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PinPostResponse pin(Long userId, Long postId) {
        if (!userRepository.existsByIdAndDeletedAtIsNull(userId)) {
            throw new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND);
        }

        Post post = postRepository.findByIdAndDeletedAtIsNull(postId).orElseThrow(
                () -> new ServiceErrorException(PostExceptionEnum.POST_NOT_FOUND));

        User postWriter = userRepository.findByIdAndDeletedAtIsNull(post.getUserId()).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        if (post.getIsPinned()) {
            post.unpin();
        } else {
            Long pinnedCount = postRepository.countByDeletedAtIsNullAndIsPinnedTrue();
            if (pinnedCount >= 10) {
                throw new ServiceErrorException(PostExceptionEnum.POST_PIN_LIMIT_EXCEEDED);
            }
            post.pin();
        }

        return new PinPostResponse(
                post.getId(),
                post.getTitle(),
                postWriter.getNickname(),
                post.getType(),
                post.getCreatedAt(),
                post.getIsPinned(),
                post.getPinnedAt());
    }
}
