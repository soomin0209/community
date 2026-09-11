package com.community.domain.file.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.file.entity.File;
import com.community.domain.file.exception.FileExceptionEnum;
import com.community.domain.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FileManagerService {

    private final FileRepository fileRepository;

    // 파일 강제 삭제
    public void delete(Long userId, Long fileId) {
        File file = fileRepository.findByIdAndDeletedAtIsNull(fileId).orElseThrow(
                () -> new ServiceErrorException(FileExceptionEnum.FILE_NOT_FOUND));

        // DB Soft Delete
        file.deleteByManager(userId);

        // 실제 파일 즉시 삭제
        try {
            Path path = Paths.get(file.getStoredPath());
            if (Files.exists(path)) Files.delete(path);
        } catch (IOException e) {
            log.warn("[FileManagerService] 파일 삭제 실패 - fileId={}, path={}", fileId, file.getStoredPath());
        }
    }
}
