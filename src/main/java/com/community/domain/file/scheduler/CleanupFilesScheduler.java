package com.community.domain.file.scheduler;

import com.community.domain.file.entity.File;
import com.community.domain.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

import static com.community.common.constant.AppConstants.FILE_RETENTION_DAYS;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupFilesScheduler {

    private final FileRepository fileRepository;

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanupOrphanFiles() {
        log.info("[CleanupFilesScheduler] 고아 파일 정리 시작");

        LocalDateTime threshold = LocalDateTime.now().minusDays(FILE_RETENTION_DAYS);
        List<File> orphanList = fileRepository.findByPostIdIsNullAndCreatedAtBefore(threshold);

        int count = 0;
        for (File file : orphanList) {
            if (deletePhysicalFile(file)) {
                file.delete();
                count++;
            }
        }

        log.info("[CleanupFilesScheduler] 고아 파일 정리 완료 - {}건", count);
    }

    @Scheduled(cron = "0 30 4 * * *")
    @Transactional(readOnly = true)
    public void cleanupDeletedFiles() {
        log.info("[CleanupFilesScheduler] 삭제 실패 파일 정리 시작");

        List<File> deletedList = fileRepository.findAllByDeletedAtIsNotNull();

        int count = 0;
        for (File file : deletedList) {
            if (deletePhysicalFile(file)) {
                count++;
            }
        }

        log.info("[CleanupFilesScheduler] 삭제 실패 파일 정리 완료 - {}건", count);
    }

    private boolean deletePhysicalFile(File file) {
        try {
            Path path = Paths.get(file.getStoredPath());
            if (Files.exists(path)) {
                Files.delete(path);
                return true;
            }
            return false;
        } catch (IOException e) {
            log.error("[CleanupFilesScheduler] 파일 정리 실패 - fileId={}, path={}", file.getId(), file.getStoredPath(), e);
            return false;
        }
    }
}
