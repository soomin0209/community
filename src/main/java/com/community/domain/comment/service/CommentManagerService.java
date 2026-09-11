package com.community.domain.comment.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.comment.entity.Comment;
import com.community.domain.comment.exception.CommentExceptionEnum;
import com.community.domain.comment.repository.CommentRepository;
import com.community.domain.user.entity.User;
import com.community.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentManagerService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    // 댓글 강제 삭제
    public void delete(Long userId, Long commentId) {
        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId).orElseThrow(
                () -> new ServiceErrorException(CommentExceptionEnum.COMMENT_NOT_FOUND));

        User writer = userRepository.findByIdAndDeletedAtIsNull(comment.getUserId()).orElse(null);
        if (writer != null) {
            writer.decreaseCommentCount();
        }

        comment.deleteByManager(userId);
    }
}
