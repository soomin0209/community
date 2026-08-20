package com.community.domain.post.dto.request;

import com.community.domain.post.enums.PostSearchType;
import com.community.domain.post.enums.PostSortType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostPageCondition {
    @PositiveOrZero(message = "페이지는 0 이상이어야 합니다")
    private int page = 0;

    @Positive(message = "페이지 크기는 1 이상이어야 합니다")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다")
    private int size = 20;

    private PostSortType sortType = PostSortType.LATEST;

    private String keyword;

    private PostSearchType searchType = PostSearchType.TITLE_CONTENT;
}
