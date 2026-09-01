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

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupFilesScheduler {

    private final FileRepository fileRepository;

    private static final int FILE_RETENTION_DAYS = 30;

    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanupOrphanFiles() {
        log.info("[CleanupFilesScheduler] 고아 파일 정리 시작");

        LocalDateTime threshold = LocalDateTime.now().minusDays(FILE_RETENTION_DAYS);
        List<File> orphanList = fileRepository.findByPostIdIsNullAndCreatedAtBefore(threshold);
        int count = deleteFiles(orphanList);

        log.info("[CleanupFilesScheduler] 고아 파일 정리 완료 - {}건", count);
    }

    @Scheduled(cron = "0 30 4 * * *")
    @Transactional
    public void cleanupDeletedFiles() {
        log.info("[CleanupFilesScheduler] 삭제 파일 정리 시작");

        LocalDateTime threshold = LocalDateTime.now().minusDays(FILE_RETENTION_DAYS);
        List<File> deletedList = fileRepository.findByDeletedAtBefore(threshold);
        int count = deleteFiles(deletedList);

        log.info("[CleanupFilesScheduler] 삭제 파일 정리 완료 - {}건", count);
    }

    private int deleteFiles(List<File> fileList) {
        int count = 0;

        for (File file : fileList) {
            try {
                Path path = Paths.get(file.getStoredPath());
                if (Files.exists(path)) Files.delete(path);

                fileRepository.delete(file);
                count++;
            } catch (IOException e) {
                log.error("[CleanupFilesScheduler] 파일 정리 실패 - fileId={}, path={}", file.getId(), file.getStoredPath(), e);
            }
        }

        return count;
    }
}
