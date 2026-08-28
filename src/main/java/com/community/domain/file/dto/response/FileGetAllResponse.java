package com.community.domain.file.dto.response;

public record FileGetAllResponse(
        Long id,
        String url,
        String originalFilename,
        Long size,
        String contentType
) {}
