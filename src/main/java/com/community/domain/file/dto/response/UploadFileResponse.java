package com.community.domain.file.dto.response;

import java.time.LocalDateTime;

public record UploadFileResponse(
        Long id,
        String url,
        String originalFilename,
        Long size,
        String contentType,
        LocalDateTime createdAt
) {}
