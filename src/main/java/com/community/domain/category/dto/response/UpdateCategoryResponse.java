package com.community.domain.category.dto.response;

import java.time.LocalDateTime;

public record UpdateCategoryResponse(
        Long id,
        String name,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
