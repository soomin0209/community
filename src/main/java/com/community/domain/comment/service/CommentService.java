package com.community.domain.comment.service;

import com.community.common.dto.CursorResponse;
import com.community.common.dto.PageResponse;
import com.community.common.exception.ServiceErrorException;
import com.community.domain.comment.dto.request.CreateCommentRequest;
import com.community.domain.comment.dto.request.CommentCursorCondition;
import com.community.domain.comment.dto.request.CommentPageCondition;
import com.community.domain.comment.dto.request.UpdateCommentRequest;
import com.community.domain.comment.dto.response.CreateCommentResponse;
import com.community.domain.comment.dto.response.GetAllCommentsResponse;
import com.community.domain.comment.dto.response.GetMyCommentsResponse;
import com.community.domain.comment.dto.response.UpdateCommentResponse;
import com.community.domain.comment.entity.Comment;
import com.community.domain.comment.exception.CommentExceptionEnum;
import com.community.domain.comment.repository.CommentRepository;
import com.community.domain.post.entity.Post;
import com.community.domain.post.exception.PostExceptionEnum;
import com.community.domain.post.repository.PostRepository;
import com.community.domain.user.entity.User;
import com.community.domain.user.exception.UserExceptionEnum;
import com.community.domain.user.repository.UserRepository;
import com.community.domain.user.service.UserRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserRankingService userRankingService;

    private static final int MAX_DEPTH = 1;

    // 댓글 등록
    public CreateCommentResponse create(Long postId, Long userId, CreateCommentRequest request) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId).orElseThrow(
                () -> new ServiceErrorException(PostExceptionEnum.POST_NOT_FOUND));

        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        int depth = 0;
        if (request.parentId() != null) {
            Comment parentComment = commentRepository.findByIdAndDeletedAtIsNull(request.parentId()).orElseThrow(
                    () -> new ServiceErrorException(CommentExceptionEnum.COMMENT_NOT_FOUND));

            if (!parentComment.getPostId().equals(postId)) {
                throw new ServiceErrorException(CommentExceptionEnum.COMMENT_INVALID_PARENT);
            }

            if (parentComment.getDepth() >= MAX_DEPTH) {
                throw new ServiceErrorException(CommentExceptionEnum.COMMENT_DEPTH_LIMIT_EXCEED);
            }

            depth = parentComment.getDepth() + 1;
        }

        Comment comment = Comment.register(
                request.parentId(),
                post.getId(),
                user.getId(),
                request.content(),
                depth
        );
        commentRepository.save(comment);

        userRankingService.recordComment(user.getId());

        return new CreateCommentResponse(
                comment.getId(),
                comment.getParentId(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }

    // 댓글 목록 조회
    @Transactional(readOnly = true)
    public CursorResponse<GetAllCommentsResponse> getAll(Long postId, CommentCursorCondition condition) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(postId).orElseThrow(
                () -> new ServiceErrorException(PostExceptionEnum.POST_NOT_FOUND));

        List<GetAllCommentsResponse> parentList = commentRepository.findParentCommentsWithCursor(
                condition.getCursor(),
                condition.getSize(),
                post.getId()
        );

        if (parentList.isEmpty()) {
            return CursorResponse.of(List.of(), null, false, condition.getSize());
        }

        boolean hasNext = parentList.size() > condition.getSize();
        List<GetAllCommentsResponse> actualParentList = hasNext ? parentList.subList(0, condition.getSize()) : parentList;

        Long nextCursor = hasNext ? actualParentList.getLast().getId() : null;

        List<Long> parentIds = actualParentList.stream()
                .map(GetAllCommentsResponse::getId)
                .toList();
        List<GetAllCommentsResponse> childList = commentRepository.findChildCommentsByParentIds(parentIds);

        Map<Long, GetAllCommentsResponse> map = new LinkedHashMap<>();
        for (GetAllCommentsResponse parent : actualParentList) {
            map.put(parent.getId(), parent);
        }
        for (GetAllCommentsResponse child : childList) {
            map.put(child.getId(), child);
        }
        for (GetAllCommentsResponse child : childList) {
            if (child.getParentId() != null) {
                map.get(child.getParentId()).addChild(child);
            }
        }

        return CursorResponse.of(new ArrayList<>(actualParentList), nextCursor, hasNext, condition.getSize());
    }

    // 내 댓글 목록 조회
    @Transactional(readOnly = true)
    public PageResponse<GetMyCommentsResponse> getMine(Long userId, CommentPageCondition condition) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        Page<GetMyCommentsResponse> page = commentRepository.findMyCommentsWithCondition(
                PageRequest.of(condition.getPage(), condition.getSize()),
                user.getId()
        );

        return PageResponse.from(page);
    }

    // 댓글 수정
    public UpdateCommentResponse update(Long userId, Long commentId, UpdateCommentRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId).orElseThrow(
                () -> new ServiceErrorException(CommentExceptionEnum.COMMENT_NOT_FOUND));

        if (!comment.getUserId().equals(user.getId())) {
            throw new ServiceErrorException(CommentExceptionEnum.COMMENT_FORBIDDEN);
        }

        comment.update(request);

        return new UpdateCommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    // 댓글 삭제
    public void delete(Long userId, Long commentId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId).orElseThrow(
                () -> new ServiceErrorException(CommentExceptionEnum.COMMENT_NOT_FOUND));

        if (!comment.getUserId().equals(user.getId())) {
            throw new ServiceErrorException(CommentExceptionEnum.COMMENT_FORBIDDEN);
        }

        comment.delete();
        user.decreaseCommentCount();
    }
}
