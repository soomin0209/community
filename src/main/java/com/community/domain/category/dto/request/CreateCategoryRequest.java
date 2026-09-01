package com.community.domain.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank(message = "카테고리명을 입력해주세요")
        @Size(max = 20, message = "카테고리명은 20자 이하여야 합니다")
        String name
) {}
