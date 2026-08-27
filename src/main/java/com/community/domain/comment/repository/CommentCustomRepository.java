package com.community.domain.comment.repository;

import com.community.domain.comment.dto.response.CommentGetAllResponse;
import com.community.domain.comment.dto.response.CommentGetMineResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommentCustomRepository {
    List<CommentGetAllResponse> findParentCommentsWithCondition(Pageable pageable, Long postId);

    List<CommentGetAllResponse> findChildCommentsByParentIds(List<Long> parentIds);

    Page<CommentGetMineResponse> findMyCommentsWithCondition(Pageable pageable, Long userId);
}
