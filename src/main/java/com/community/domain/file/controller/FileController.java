package com.community.domain.file.controller;

import com.community.common.config.security.CustomUserDetails;
import com.community.common.dto.BaseResponse;
import com.community.domain.file.dto.response.DownloadFileResponse;
import com.community.domain.file.dto.response.UploadFileResponse;
import com.community.domain.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    // 파일 업로드
    @PostMapping("/upload")
    public ResponseEntity<BaseResponse<List<UploadFileResponse>>> upload(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart("files") List<MultipartFile> files
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                HttpStatus.CREATED.name(), null, fileService.upload(userId, files)));
    }

    // 파일 다운로드
    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> download(@PathVariable Long fileId) {
        DownloadFileResponse response = fileService.download(fileId);

        String encodedFilename = URLEncoder.encode(response.originalFilename(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFilename)
                .header(HttpHeaders.CONTENT_TYPE, response.contentType())
                .contentLength(response.size())
                .body(response.resource());
    }
}
