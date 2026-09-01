package com.community.domain.category.dto.response;

import java.time.LocalDateTime;

public record CreateCategoryResponse(
        Long id,
        String name,
        LocalDateTime createdAt
) {}
