package com.community.domain.file.dto.response;

import org.springframework.core.io.Resource;

public record DownloadFileResponse(
        Resource resource,
        String originalFilename,
        Long size,
        String contentType
) {}
