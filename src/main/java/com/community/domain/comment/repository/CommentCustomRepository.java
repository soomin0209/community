package com.community.domain.comment.repository;

import com.community.domain.comment.dto.response.GetAllCommentsResponse;
import com.community.domain.comment.dto.response.GetMyCommentsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommentCustomRepository {
    List<GetAllCommentsResponse> findParentCommentsWithCursor(Long cursor, int size, Long postId);

    List<GetAllCommentsResponse> findChildCommentsByParentIds(List<Long> parentIds);

    Page<GetMyCommentsResponse> findMyCommentsWithCondition(Pageable pageable, Long userId);
}
