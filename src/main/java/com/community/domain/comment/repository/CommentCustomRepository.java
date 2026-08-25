package com.community.domain.comment.repository;

import com.community.domain.comment.dto.response.CommentGetAllResponse;
import com.community.domain.comment.dto.response.CommentGetMineResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentCustomRepository {
    Page<CommentGetAllResponse> findCommentsWithCondition(Pageable pageable, Long postId);

    Page<CommentGetMineResponse> findMyCommentsWithCondition(Pageable pageable, Long userId);
}
