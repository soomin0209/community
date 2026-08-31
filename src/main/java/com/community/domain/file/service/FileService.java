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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FileService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    private static final String UPLOAD_DIR = "uploads/";
    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            "jpg", "jpeg", "png", "gif", "webp",
            "pdf", "txt", "doc", "docx", "xls", "xlsx",
            "json", "xml", "csv", "md", "zip"
    );
    private static final List<String> BLOCKED_EXTENSIONS = List.of(
            "exe", "bat", "sh", "jar", "war", "dll",
            "jsp", "php", "asp", "aspx"
    );
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final int MAX_FILES_COUNT = 10;

    // 파일 업로드
    public List<UploadFileResponse> upload(Long userId, List<MultipartFile> files) {
        if (!userRepository.existsByIdAndDeletedAtIsNull(userId)) {
            throw new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND);
        }

        if (files.size() > MAX_FILES_COUNT) {
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

    // 파일 첨부
    public void attachFiles(Long userId, Long postId, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }

        List<File> files = new ArrayList<>();
        for (Long fileId : fileIds) {
            File file = fileRepository.findByIdAndDeletedAtIsNull(fileId).orElseThrow(
                    () -> new ServiceErrorException(FileExceptionEnum.FILE_NOT_FOUND));

            if (!file.getUserId().equals(userId)) {
                throw new ServiceErrorException(FileExceptionEnum.FILE_FORBIDDEN);
            }

            if (file.getPostId() != null) {
                throw new ServiceErrorException(FileExceptionEnum.FILE_ALREADY_ATTACHED);
            }

            files.add(file);
        }

        for (File file : files) {
            file.attachToPost(postId);
        }
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
        if (!Files.exists(path)) {
            throw new ServiceErrorException(FileExceptionEnum.FILE_NOT_FOUND);
        }

        Resource resource = new FileSystemResource(path);

        return new DownloadFileResponse(
                resource,
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType()
        );
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

        file.delete();
    }

    // 파일 저장
    private UploadFileResponse uploadSingle(Long userId, MultipartFile file) {
        validateFile(file);

        try {
            String storedFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path uploadPath = Paths.get(UPLOAD_DIR);
            Path filePath = uploadPath.resolve(storedFileName);

            Files.createDirectories(uploadPath);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            File fileEntity = File.register(
                    userId,
                    file.getOriginalFilename(),
                    UPLOAD_DIR + storedFileName,
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
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ServiceErrorException(FileExceptionEnum.FILE_SIZE_EXCEEDED);
        }

        String extension = getExtension(file.getOriginalFilename());
        if (BLOCKED_EXTENSIONS.contains(extension)) {
            throw new ServiceErrorException(FileExceptionEnum.BLOCKED_FILE_EXTENSION);
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
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
}
