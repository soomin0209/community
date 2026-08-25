package com.community.domain.comment.service;

import com.community.common.dto.PageResponse;
import com.community.common.exception.ServiceErrorException;
import com.community.domain.comment.dto.request.CommentCreateRequest;
import com.community.domain.comment.dto.request.CommentPageCondition;
import com.community.domain.comment.dto.response.CommentCreateResponse;
import com.community.domain.comment.dto.response.CommentGetAllResponse;
import com.community.domain.comment.entity.Comment;
import com.community.domain.comment.repository.CommentRepository;
import com.community.domain.post.entity.Post;
import com.community.domain.post.exception.PostExceptionEnum;
import com.community.domain.post.repository.PostRepository;
import com.community.domain.user.entity.User;
import com.community.domain.user.exception.UserExceptionEnum;
import com.community.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public CommentCreateResponse create(Long postId, Long userId, CommentCreateRequest request) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId).orElseThrow(
                () -> new ServiceErrorException(PostExceptionEnum.POST_NOT_FOUND));

        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        Comment comment = Comment.register(post.getId(), user.getId(), request.content());
        commentRepository.save(comment);

        return new CommentCreateResponse(
                comment.getId(),
                user.getNickname(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentGetAllResponse> getAll(Long postId, CommentPageCondition condition) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId).orElseThrow(
                () -> new ServiceErrorException(PostExceptionEnum.POST_NOT_FOUND));

        Page<CommentGetAllResponse> page = commentRepository.findCommentsWithCondition(
                PageRequest.of(condition.getPage(), condition.getSize()),
                post.getId(),
                null
        );

        return PageResponse.from(page);
    }
}
