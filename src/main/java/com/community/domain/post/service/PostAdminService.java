package com.community.domain.post.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.post.dto.response.PostPinResponse;
import com.community.domain.post.entity.Post;
import com.community.domain.post.enums.PostType;
import com.community.domain.post.exception.PostExceptionEnum;
import com.community.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PostAdminService {

    private final PostRepository postRepository;

    public PostPinResponse pin(Long postId) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId).orElseThrow(
                () -> new ServiceErrorException(PostExceptionEnum.POST_NOT_FOUND));

        if (post.getType() != PostType.NOTICE) {
            throw new ServiceErrorException(PostExceptionEnum.POST_NOT_NOTICE);
        }

        if (post.getIsPinned()) {
            post.unpin();
        } else {
            post.pin();
        }

        return new PostPinResponse(post.getId(), post.getIsPinned(), post.getPinnedAt());
    }
}
