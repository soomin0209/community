package com.community.domain.file.controller;

import com.community.common.config.security.CustomUserDetails;
import com.community.common.dto.BaseResponse;
import com.community.domain.file.service.FileManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/files")
public class FileManagerController {

    private final FileManagerService fileManagerService;

    // 파일 강제 삭제
    @DeleteMapping("/{fileId}")
    public ResponseEntity<BaseResponse<Void>> delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long fileId
    ) {
        Long userId = userDetails.getUserId();
        fileManagerService.delete(userId, fileId);
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, null));
    }
}
