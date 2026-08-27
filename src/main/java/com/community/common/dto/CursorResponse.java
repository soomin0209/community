package com.community.common.dto;

import java.util.List;

public record CursorResponse<T>(
        List<T> content,
        Long nextCursor,
        boolean hasNext,
        int size
) {
    public static <T> CursorResponse<T> of(List<T> content, Long nextCursor, boolean hasNext, int size) {
        return new CursorResponse<>(content, nextCursor, hasNext, size);
    }
}
