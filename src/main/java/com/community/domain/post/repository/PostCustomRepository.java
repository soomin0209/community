package com.community.domain.post.repository;

import com.community.domain.post.dto.response.GetAllPostsResponse;
import com.community.domain.post.enums.PostSearchType;
import com.community.domain.post.enums.PostSortType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostCustomRepository {
    Page<GetAllPostsResponse> findPostsWithCondition(
            Pageable pageable,
            PostSortType sortType,
            String keyword,
            PostSearchType searchType,
            Long userId
    );
}
