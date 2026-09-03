package com.community.domain.file.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.file.dto.response.DownloadFileResponse;
import com.community.domain.file.dto.response.GetAllFilesResponse;
import com.community.domain.file.dto.response.UploadFileResponse;
import com.community.domain.file.entity.File;
import com.community.domain.file.exception.FileExceptionEnum;
import com.community.domain.file.repository.FileRepository;
import com.community.domain.user.exception.UserExceptionEnum;
import com.community.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static com.community.common.constant.AppConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FileService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    // 파일 업로드
    public List<UploadFileResponse> upload(Long userId, List<MultipartFile> files) {
        if (!userRepository.existsByIdAndDeletedAtIsNull(userId)) {
            throw new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND);
        }

        if (files.size() > FILE_MAX_COUNT) {
            throw new ServiceErrorException(FileExceptionEnum.FILE_COUNT_EXCEEDED);
        }

        List<UploadFileResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                UploadFileResponse response = uploadSingle(userId, file);
                responses.add(response);
            }
        }

        return responses;
    }

    // 파일 목록 조회
    @Transactional(readOnly = true)
    public List<GetAllFilesResponse> getAll(Long postId) {
        List<File> files = fileRepository.findByPostIdAndDeletedAtIsNull(postId);

        List<GetAllFilesResponse> responses = new ArrayList<>();
        for (File file : files) {
            GetAllFilesResponse response = new GetAllFilesResponse(
                    file.getId(),
                    file.getStoredPath(),
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType()
            );
            responses.add(response);
        }

        return responses;
    }

    // 파일 다운로드
    public DownloadFileResponse download(Long fileId) {
        File file = fileRepository.findByIdAndDeletedAtIsNull(fileId).orElseThrow(
                () -> new ServiceErrorException(FileExceptionEnum.FILE_NOT_FOUND));

        Path path = Paths.get(file.getStoredPath());
        validateFileExists(path);

        Resource resource = new FileSystemResource(path);

        return new DownloadFileResponse(
                resource,
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType()
        );
    }

    // 파일 첨부
    public void attachFiles(Long userId, Long postId, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }

        List<File> files = fileRepository.findAllByIdInAndDeletedAtIsNull(fileIds);
        if (files.size() != fileIds.size()) {
            throw new ServiceErrorException(FileExceptionEnum.FILE_NOT_FOUND);
        }

        for (File file : files) {
            if (!file.getUserId().equals(userId)) {
                throw new ServiceErrorException(FileExceptionEnum.FILE_FORBIDDEN);
            }

            if (file.getPostId() != null) {
                throw new ServiceErrorException(FileExceptionEnum.FILE_ALREADY_ATTACHED);
            }

            Path path = Paths.get(file.getStoredPath());
            validateFileExists(path);
        }

        for (File file : files) {
            file.attachToPost(postId);
        }
    }

    // 첨부된 파일 수정
    public void updateFiles(Long userId, Long postId, List<Long> updatedFileIds) {
        List<File> attachedFiles = fileRepository.findByPostIdAndDeletedAtIsNull(postId);
        List<Long> attachedFileIds = attachedFiles.stream()
                .map(File::getId)
                .toList();

        if (updatedFileIds == null) {
            updatedFileIds = List.of();
        }
        if (new HashSet<>(attachedFileIds).equals(new HashSet<>(updatedFileIds))) {
            return;
        }

        // attached에는 있지만 updated에는 없는 파일 제거
        for (File attachedFile : attachedFiles) {
            if (!updatedFileIds.contains(attachedFile.getId())) {
                delete(userId, attachedFile.getId());
            }
        }

        // updated에는 있지만 attached에는 없는 파일 추가
        List<Long> fileIdsToAttach = new ArrayList<>();
        for (Long updatedFileId : updatedFileIds) {
            if (!attachedFileIds.contains(updatedFileId)) {
                fileIdsToAttach.add(updatedFileId);
            }
        }
        if (!fileIdsToAttach.isEmpty()) {
            attachFiles(userId, postId, fileIdsToAttach);
        }
    }

    // 파일 삭제
    public void delete(Long userId, Long fileId) {
        if (!userRepository.existsByIdAndDeletedAtIsNull(userId)) {
            throw new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND);
        }

        File file = fileRepository.findByIdAndDeletedAtIsNull(fileId).orElseThrow(
                () -> new ServiceErrorException(FileExceptionEnum.FILE_NOT_FOUND));

        if (!file.getUserId().equals(userId)) {
            throw new ServiceErrorException(FileExceptionEnum.FILE_FORBIDDEN);
        }

        // DB Soft Delete
        file.delete();

        // 실제 파일 즉시 삭제
        try {
            Path path = Paths.get(file.getStoredPath());
            if (Files.exists(path)) Files.delete(path);
        } catch (IOException e) {
            log.warn("[FileService] 파일 삭제 실패 - fileId={}, path={}", fileId, file.getStoredPath());
        }
    }

    // 파일 저장
    private UploadFileResponse uploadSingle(Long userId, MultipartFile file) {
        validateFile(file);

        try {
            String storedFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path uploadPath = Paths.get(FILE_UPLOAD_DIR);
            Path filePath = uploadPath.resolve(storedFileName);

            Files.createDirectories(uploadPath);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            File fileEntity = File.register(
                    userId,
                    file.getOriginalFilename(),
                    FILE_UPLOAD_DIR + storedFileName,
                    file.getSize(),
                    file.getContentType()
            );
            fileRepository.save(fileEntity);

            return new UploadFileResponse(
                    fileEntity.getId(),
                    fileEntity.getStoredPath(),
                    fileEntity.getOriginalFilename(),
                    fileEntity.getSize(),
                    fileEntity.getContentType(),
                    fileEntity.getCreatedAt()
            );
        } catch (IOException e) {
            throw new ServiceErrorException(FileExceptionEnum.FILE_UPLOAD_FAILED);
        }
    }

    // 파일 검증
    private void validateFile(MultipartFile file) {
        if (file.getSize() > FILE_MAX_SIZE) {
            throw new ServiceErrorException(FileExceptionEnum.FILE_SIZE_EXCEEDED);
        }

        String extension = getExtension(file.getOriginalFilename());
        if (FILE_BLOCKED_EXTENSIONS.contains(extension)) {
            throw new ServiceErrorException(FileExceptionEnum.BLOCKED_FILE_EXTENSION);
        }
        if (!FILE_ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ServiceErrorException(FileExceptionEnum.INVALID_FILE_EXTENSION);
        }
    }

    // 확장자 추출
    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new ServiceErrorException(FileExceptionEnum.INVALID_FILE_EXTENSION);
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    // 실제 파일 존재 검증
    private void validateFileExists(Path path) {
        if (!Files.exists(path)) {
            throw new ServiceErrorException(FileExceptionEnum.FILE_NOT_FOUND);
        }
    }
}
