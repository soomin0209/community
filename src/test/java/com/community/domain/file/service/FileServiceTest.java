package com.community.domain.file.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.board.service.BoardService;
import com.community.domain.file.dto.response.DownloadFileResponse;
import com.community.domain.file.dto.response.GetAllFilesResponse;
import com.community.domain.file.dto.response.UploadFileResponse;
import com.community.domain.file.entity.File;
import com.community.domain.file.exception.FileExceptionEnum;
import com.community.domain.file.repository.FileRepository;
import com.community.domain.post.entity.Post;
import com.community.domain.post.enums.PostType;
import com.community.domain.post.exception.PostExceptionEnum;
import com.community.domain.post.repository.PostRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.community.common.constant.AppConstants.FILE_MAX_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @InjectMocks
    private FileService fileService;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private BoardService boardService;

    private final List<String> createdFilePaths = new ArrayList<>();

    @AfterEach
    void tearDown() throws IOException {
        for (String filePath : createdFilePaths) {
            Files.delete(Paths.get(filePath));
        }
    }


    // ========== 파일 업로드 ==========
    @Test
    @DisplayName("파일 업로드 성공")
    void upload_success() {
        // given
        MockMultipartFile file1 = new MockMultipartFile("file1", "테스트 파일.txt", "text/plain", "테스트 파일 내용".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file2", "테스트 파일.txt", "text/plain", "테스트 파일 내용".getBytes());
        List<MultipartFile> files = List.of(file1, file2);

        // when
        List<UploadFileResponse> responses = fileService.upload(1L, files);

        // then
        assertThat(responses).hasSize(2);

        responses.forEach(response -> createdFilePaths.add(response.url()));
    }

    @Test
    @DisplayName("파일 업로드 성공 - 빈 파일")
    void upload_success_filesIsEmpty() {
        // given
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "text/plain", new byte[0]);
        List<MultipartFile> files = List.of(emptyFile);

        // when
        List<UploadFileResponse> responses = fileService.upload(1L, files);

        // then
        assertThat(responses).hasSize(0);
    }

    @Test
    @DisplayName("파일 업로드 실패 - 파일 개수 초과")
    void upload_fail_countExceeded() {
        // given
        MockMultipartFile file1 = new MockMultipartFile("file1", "테스트 파일.txt", "text/plain", "테스트 파일 내용".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file2", "테스트 파일.txt", "text/plain", "테스트 파일 내용".getBytes());
        MockMultipartFile file3 = new MockMultipartFile("file3", "테스트 파일.txt", "text/plain", "테스트 파일 내용".getBytes());
        MockMultipartFile file4 = new MockMultipartFile("file4", "테스트 파일.txt", "text/plain", "테스트 파일 내용".getBytes());
        MockMultipartFile file5 = new MockMultipartFile("file5", "테스트 파일.txt", "text/plain", "테스트 파일 내용".getBytes());
        MockMultipartFile file6 = new MockMultipartFile("file6", "테스트 파일.txt", "text/plain", "테스트 파일 내용".getBytes());
        MockMultipartFile file7 = new MockMultipartFile("file7", "테스트 파일.txt", "text/plain", "테스트 파일 내용".getBytes());
        MockMultipartFile file8 = new MockMultipartFile("file8", "테스트 파일.txt", "text/plain", "테스트 파일 내용".getBytes());
        MockMultipartFile file9 = new MockMultipartFile("file9", "테스트 파일.txt", "text/plain", "테스트 파일 내용".getBytes());
        MockMultipartFile file10 = new MockMultipartFile("file10", "테스트 파일.txt", "text/plain", "테스트 파일 내용".getBytes());
        MockMultipartFile file11 = new MockMultipartFile("file11", "테스트 파일.txt", "text/plain", "테스트 파일 내용".getBytes());
        List<MultipartFile> files = List.of(file1, file2, file3, file4, file5, file6, file7, file8, file9, file10, file11);

        // when & then
        assertThatThrownBy(() -> fileService.upload(1L, files))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(FileExceptionEnum.FILE_COUNT_EXCEEDED.getMessage());
    }

    @Test
    @DisplayName("파일 업로드 실패 - 파일 크기 초과")
    void upload_fail_sizeExceeded() {
        // given
        MockMultipartFile file = new MockMultipartFile("file", "테스트 파일.txt", "text/plain", new byte[(int) FILE_MAX_SIZE + 1]);
        List<MultipartFile> files = List.of(file);

        // when & then
        assertThatThrownBy(() -> fileService.upload(1L, files))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(FileExceptionEnum.FILE_SIZE_EXCEEDED.getMessage());
    }

    @Test
    @DisplayName("파일 업로드 실패 - 차단된 확장자")
    void upload_fail_blockedExtension() {
        // given
        MockMultipartFile file = new MockMultipartFile("file", "실행 파일.exe", "application/octet-stream", "테스트 파일 내용".getBytes());
        List<MultipartFile> files = List.of(file);

        // when & then
        assertThatThrownBy(() -> fileService.upload(1L, files))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(FileExceptionEnum.BLOCKED_FILE_EXTENSION.getMessage());
    }

    @Test
    @DisplayName("파일 업로드 실패 - 허용되지 않은 확장자")
    void upload_fail_invalidExtension() {
        // given
        MockMultipartFile file = new MockMultipartFile("file", "테스트.xyz", "application/octet-stream", "테스트 파일 내용".getBytes());
        List<MultipartFile> files = List.of(file);

        // when & then
        assertThatThrownBy(() -> fileService.upload(1L, files))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(FileExceptionEnum.INVALID_FILE_EXTENSION.getMessage());
    }

    @Test
    @DisplayName("파일 업로드 실패 - 확장자명 없음")
    void upload_fail_noExtension() {
        // given
        MockMultipartFile file = new MockMultipartFile("file", "확장자없음", "application/octet-stream", "테스트 파일 내용".getBytes());
        List<MultipartFile> files = List.of(file);

        // when & then
        assertThatThrownBy(() -> fileService.upload(1L, files))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(FileExceptionEnum.INVALID_FILE_EXTENSION.getMessage());
    }


    // ========== 파일 목록 조회 ==========
    @Test
    @DisplayName("파일 목록 조회 성공")
    void getAll_success() {
        // given
        Long userId = 1L;
        Long postId = 1L;

        File file1 = File.register(userId, "테스트 파일1.txt", "uploads/2026/09/18/uuid1", 1000L, "text/plain");
        File file2 = File.register(userId, "테스트 파일2.txt", "uploads/2026/09/18/uuid2", 2000L, "text/plain");

        given(fileRepository.findByPostIdAndDeletedAtIsNull(postId)).willReturn(List.of(file1, file2));

        // when
        List<GetAllFilesResponse> responses = fileService.getAll(postId);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.getFirst().originalFilename()).isEqualTo(file1.getOriginalFilename());
        assertThat(responses.getLast().originalFilename()).isEqualTo(file2.getOriginalFilename());
    }


    // ========== 파일 다운로드 ==========
    @Test
    @DisplayName("파일 다운로드 성공")
    void download_success() {
        // given
        Long userId = 1L;
        Long writerId = 2L;

        Post post = Post.register(2L, 1L, "Spring Boot 학습 자료", "유용한 Spring Boot 학습 자료 모음입니다.", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);
        ReflectionTestUtils.setField(post, "userId", writerId);

        File file = File.register(writerId, "테스트 파일.txt", "uploads/2026/09/18/uuid", 1000L, "text/plain");
        ReflectionTestUtils.setField(file, "id", 1L);
        ReflectionTestUtils.setField(file, "postId", post.getId());

        Path path = Paths.get(file.getStoredPath());

        given(fileRepository.findByIdAndDeletedAtIsNull(file.getId())).willReturn(Optional.of(file));
        given(postRepository.findByIdAndDeletedAtIsNull(file.getPostId())).willReturn(Optional.of(post));

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(path)).thenReturn(true);

            // when
            DownloadFileResponse response = fileService.download(file.getId(), userId);

            // then
            assertThat(response.originalFilename()).isEqualTo(file.getOriginalFilename());
            assertThat(response.size()).isEqualTo(file.getSize());
            assertThat(response.contentType()).isEqualTo(file.getContentType());
        }
    }

    @Test
    @DisplayName("파일 다운로드 성공 - 첨부된 내 파일")
    void download_success_attachedMyFile() {
        // given
        Long userId = 1L;

        Post post = Post.register(userId, 1L, "Spring Boot 학습 자료", "유용한 Spring Boot 학습 자료 모음입니다.", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);
        ReflectionTestUtils.setField(post, "userId", userId);

        File file = File.register(userId, "테스트 파일.txt", "uploads/2026/09/18/uuid", 1000L, "text/plain");
        ReflectionTestUtils.setField(file, "id", 1L);
        ReflectionTestUtils.setField(file, "postId", post.getId());

        Path path = Paths.get(file.getStoredPath());

        given(fileRepository.findByIdAndDeletedAtIsNull(file.getId())).willReturn(Optional.of(file));
        given(postRepository.findByIdAndDeletedAtIsNull(file.getPostId())).willReturn(Optional.of(post));

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(path)).thenReturn(true);

            // when
            DownloadFileResponse response = fileService.download(file.getId(), userId);

            // then
            assertThat(response.originalFilename()).isEqualTo(file.getOriginalFilename());
            assertThat(response.size()).isEqualTo(file.getSize());
            assertThat(response.contentType()).isEqualTo(file.getContentType());
        }
    }

    @Test
    @DisplayName("파일 다운로드 성공 - 첨부되지 않은 내 파일")
    void download_success_unattachedMyFile() {
        // given
        Long userId = 1L;

        File file = File.register(userId, "테스트 파일.txt", "uploads/2026/09/18/uuid", 1000L, "text/plain");
        ReflectionTestUtils.setField(file, "id", 1L);

        Path path = Paths.get(file.getStoredPath());

        given(fileRepository.findByIdAndDeletedAtIsNull(file.getId())).willReturn(Optional.of(file));

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(path)).thenReturn(true);

            // when
            DownloadFileResponse response = fileService.download(file.getId(), userId);

            // then
            assertThat(response.originalFilename()).isEqualTo(file.getOriginalFilename());
            assertThat(response.size()).isEqualTo(file.getSize());
            assertThat(response.contentType()).isEqualTo(file.getContentType());
        }
    }

    @Test
    @DisplayName("파일 다운로드 실패 - 파일 없음")
    void download_fail_fileNotFound() {
        // given
        Long fileId = 99L;

        given(fileRepository.findByIdAndDeletedAtIsNull(fileId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> fileService.download(fileId, 1L))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(FileExceptionEnum.FILE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("파일 다운로드 실패 - 게시물 없음")
    void download_fail_postNotFound() {
        // given
        Long userId = 1L;
        Long postId = 99L;

        File file = File.register(userId, "테스트 파일.txt", "uploads/2026/09/18/uuid", 1000L, "text/plain");
        ReflectionTestUtils.setField(file, "id", 1L);
        ReflectionTestUtils.setField(file, "postId", postId);

        given(fileRepository.findByIdAndDeletedAtIsNull(file.getId())).willReturn(Optional.of(file));
        given(postRepository.findByIdAndDeletedAtIsNull(postId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> fileService.download(file.getId(), userId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(PostExceptionEnum.POST_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("파일 다운로드 실패 - 첨부되지 않은 남의 파일")
    void download_fail_forbidden() {
        // given
        Long userId = 1L;

        File file = File.register(2L, "테스트 파일.txt", "uploads/2026/09/18/uuid", 1000L, "text/plain");
        ReflectionTestUtils.setField(file, "id", 1L);

        given(fileRepository.findByIdAndDeletedAtIsNull(file.getId())).willReturn(Optional.of(file));

        // when & then
        assertThatThrownBy(() -> fileService.download(file.getId(), userId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(FileExceptionEnum.FILE_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("파일 다운로드 실패 - 파일시스템에 물리 파일 없음")
    void download_fail_physicalFileNotFound() {
        // given
        Long userId = 1L;

        Post post = Post.register(userId, 1L, "Spring Boot 학습 자료", "유용한 Spring Boot 학습 자료 모음입니다.", PostType.GENERAL);
        ReflectionTestUtils.setField(post, "id", 1L);
        ReflectionTestUtils.setField(post, "userId", userId);

        File file = File.register(userId, "테스트 파일.txt", "uploads/2026/09/18/uuid", 1000L, "text/plain");
        ReflectionTestUtils.setField(file, "id", 1L);
        ReflectionTestUtils.setField(file, "postId", post.getId());

        Path path = Paths.get(file.getStoredPath());

        given(fileRepository.findByIdAndDeletedAtIsNull(file.getId())).willReturn(Optional.of(file));
        given(postRepository.findByIdAndDeletedAtIsNull(file.getPostId())).willReturn(Optional.of(post));

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(path)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> fileService.download(file.getId(), userId))
                    .isInstanceOf(ServiceErrorException.class)
                    .hasMessage(FileExceptionEnum.FILE_NOT_FOUND.getMessage());
        }
    }


    // ========== 파일 첨부 ==========
    @Test
    @DisplayName("파일 첨부 성공")
    void attachFiles_success() {
        // given
        Long userId = 1L;
        Long postId = 1L;

        File file1 = File.register(userId, "테스트 파일1.txt", "uploads/2026/09/18/uuid1", 1000L, "text/plain");
        ReflectionTestUtils.setField(file1, "id", 1L);

        File file2 = File.register(userId, "테스트 파일2.txt", "uploads/2026/09/18/uuid2", 2000L, "text/plain");
        ReflectionTestUtils.setField(file2, "id", 2L);

        List<Long> fileIds = List.of(file1.getId(), file2.getId());

        Path path1 = Paths.get(file1.getStoredPath());
        Path path2 = Paths.get(file2.getStoredPath());

        given(fileRepository.findAllByIdInAndDeletedAtIsNull(fileIds)).willReturn(List.of(file1, file2));

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(path1)).thenReturn(true);
            mockedFiles.when(() -> Files.exists(path2)).thenReturn(true);

            // when
            fileService.attachFiles(userId, postId, fileIds);
        }

        // then
        assertThat(file1.getPostId()).isEqualTo(postId);
        assertThat(file2.getPostId()).isEqualTo(postId);
    }

    @Test
    @DisplayName("파일 첨부 성공 - fileIds가 null")
    void attachFiles_success_fileIdsIsNull() {
        // given
        List<Long> fileIds = null;

        // when
        fileService.attachFiles(1L, 1L, fileIds);

        // then
        verify(fileRepository, never()).findAllByIdInAndDeletedAtIsNull(any());
    }

    @Test
    @DisplayName("파일 첨부 실패 - 파일 ID 중 없는 파일 있음")
    void attachFiles_fail_fileNotFound() {
        // given
        Long userId = 1L;
        Long postId = 1L;

        File file1 = File.register(userId, "테스트 파일1.txt", "uploads/2026/09/18/uuid1", 1000L, "text/plain");
        ReflectionTestUtils.setField(file1, "id", 1L);

        List<Long> fileIds = List.of(file1.getId(), 2L);

        given(fileRepository.findAllByIdInAndDeletedAtIsNull(fileIds)).willReturn(List.of(file1));

        // when & then
        assertThatThrownBy(() -> fileService.attachFiles(userId, postId, fileIds))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(FileExceptionEnum.FILE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("파일 첨부 실패 - 내 파일 아님")
    void attachFiles_fail_notMyFiles() {
        // given
        Long userId = 1L;

        File file = File.register(2L, "테스트 파일.txt", "uploads/2026/09/18/uuid", 1000L, "text/plain");
        ReflectionTestUtils.setField(file, "id", 1L);

        List<Long> fileIds = List.of(file.getId());

        given(fileRepository.findAllByIdInAndDeletedAtIsNull(fileIds)).willReturn(List.of(file));

        // when & then
        assertThatThrownBy(() -> fileService.attachFiles(userId, 1L, fileIds))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(FileExceptionEnum.FILE_FORBIDDEN.getMessage());
    }

    @Test
    @DisplayName("파일 첨부 실패 - 이미 다른 게시물에 첨부된 파일")
    void attachFiles_fail_alreadyAttached() {
        // given
        Long userId = 1L;

        File file = File.register(userId, "테스트 파일.txt", "uploads/2026/09/18/uuid", 1000L, "text/plain");
        ReflectionTestUtils.setField(file, "id", 1L);
        ReflectionTestUtils.setField(file, "postId", 2L);

        List<Long> fileIds = List.of(file.getId());

        given(fileRepository.findAllByIdInAndDeletedAtIsNull(fileIds)).willReturn(List.of(file));

        // when & then
        assertThatThrownBy(() -> fileService.attachFiles(userId, 1L, fileIds))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(FileExceptionEnum.FILE_ALREADY_ATTACHED.getMessage());

    }

    @Test
    @DisplayName("파일 첨부 실패 - 파일시스템에 물리 파일 없음")
    void attachFiles_fail_physicalFileNotFound() {
        // given
        Long userId = 1L;

        File file = File.register(userId, "테스트 파일.txt", "uploads/2026/09/18/uuid", 1000L, "text/plain");
        ReflectionTestUtils.setField(file, "id", 1L);

        List<Long> fileIds = List.of(file.getId());

        Path path = Paths.get(file.getStoredPath());

        given(fileRepository.findAllByIdInAndDeletedAtIsNull(fileIds)).willReturn(List.of(file));

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(path)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> fileService.attachFiles(userId, 1L, fileIds))
                    .isInstanceOf(ServiceErrorException.class)
                    .hasMessage(FileExceptionEnum.FILE_NOT_FOUND.getMessage());
        }
    }


    // ========== 파일 수정 ==========
    @Test
    @DisplayName("파일 수정 성공 - 변경 사항 없음")
    void updateFiles_success_noChange() {
        // given
        Long userId = 1L;
        Long postId = 1L;

        File file = File.register(userId, "테스트 파일.txt", "uploads/2026/09/18/uuid", 1000L, "text/plain");
        ReflectionTestUtils.setField(file, "id", 1L);
        ReflectionTestUtils.setField(file, "postId", postId);

        List<Long> updatedFileIds = List.of(file.getId());

        given(fileRepository.findByPostIdAndDeletedAtIsNull(postId)).willReturn(List.of(file));

        // when
        fileService.updateFiles(userId, postId, updatedFileIds);

        // then
        verify(fileRepository, never()).findByIdAndDeletedAtIsNull(any());
        verify(fileRepository, never()).findAllByIdInAndDeletedAtIsNull(any());
    }

    @Test
    @DisplayName("파일 수정 성공 - updatedFileIds가 null")
    void updateFiles_success_updatedFileIdsIsNull() {
        // given
        Long userId = 1L;
        Long postId = 1L;

        File file = File.register(userId, "테스트 파일.txt", "uploads/2026/09/18/uuid", 1000L, "text/plain");
        ReflectionTestUtils.setField(file, "id", 1L);
        ReflectionTestUtils.setField(file, "postId", postId);

        List<Long> updatedFileIds = null;

        given(fileRepository.findByPostIdAndDeletedAtIsNull(postId)).willReturn(List.of(file));
        given(fileRepository.findByIdAndDeletedAtIsNull(file.getId())).willReturn(Optional.of(file));

        // when
        fileService.updateFiles(userId, postId, updatedFileIds);

        // then
        assertThat(file.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("파일 수정 성공 - 제거만")
    void updateFiles_success_onlyRemove() {
        // given
        Long userId = 1L;
        Long postId = 1L;

        File file1 = File.register(userId, "테스트 파일1.txt", "uploads/2026/09/18/uuid1", 1000L, "text/plain");
        ReflectionTestUtils.setField(file1, "id", 1L);
        ReflectionTestUtils.setField(file1, "postId", postId);

        File file2 = File.register(userId, "테스트 파일2.txt", "uploads/2026/09/18/uuid2", 2000L, "text/plain");
        ReflectionTestUtils.setField(file2, "id", 2L);
        ReflectionTestUtils.setField(file2, "postId", postId);

        List<Long> updatedFileIds = List.of(file1.getId());

        given(fileRepository.findByPostIdAndDeletedAtIsNull(postId)).willReturn(List.of(file1, file2));
        given(fileRepository.findByIdAndDeletedAtIsNull(file2.getId())).willReturn(Optional.of(file2));

        // when
        fileService.updateFiles(userId, postId, updatedFileIds);

        // then
        assertThat(file1.getDeletedAt()).isNull();
        assertThat(file2.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("파일 수정 성공 - 추가만")
    void updateFiles_success_onlyAdd() {
        // given
        Long userId = 1L;
        Long postId = 1L;

        File existingFile = File.register(userId, "기존 파일.txt", "uploads/2026/09/18/uuid1", 1000L, "text/plain");
        ReflectionTestUtils.setField(existingFile, "id", 1L);
        ReflectionTestUtils.setField(existingFile, "postId", postId);

        File newFile = File.register(userId, "새 파일.txt", "uploads/2026/09/18/uuid2", 2000L, "text/plain");
        ReflectionTestUtils.setField(newFile, "id", 2L);

        List<Long> updatedFileIds = List.of(existingFile.getId(), newFile.getId());

        Path newFilePath = Paths.get(newFile.getStoredPath());

        given(fileRepository.findByPostIdAndDeletedAtIsNull(postId)).willReturn(List.of(existingFile));
        given(fileRepository.findAllByIdInAndDeletedAtIsNull(List.of(newFile.getId()))).willReturn(List.of(newFile));

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(newFilePath)).thenReturn(true);

            // when
            fileService.updateFiles(userId, postId, updatedFileIds);
        }

        // then
        assertThat(newFile.getPostId()).isEqualTo(postId);
    }

    @Test
    @DisplayName("파일 수정 성공 - 제거 + 추가")
    void updateFiles_success_removeAndAdd() {
        // given
        Long userId = 1L;
        Long postId = 1L;

        File oldFile = File.register(userId, "기존 파일.txt", "uploads/2026/09/18/uuid1", 1000L, "text/plain");
        ReflectionTestUtils.setField(oldFile, "id", 1L);
        ReflectionTestUtils.setField(oldFile, "postId", postId);

        File newFile = File.register(userId, "새 파일.txt", "uploads/2026/09/18/uuid2", 2000L, "text/plain");
        ReflectionTestUtils.setField(newFile, "id", 2L);

        List<Long> updatedFileIds = List.of(newFile.getId());

        Path oldFilePath = Paths.get(oldFile.getStoredPath());
        Path newFilePath = Paths.get(newFile.getStoredPath());

        given(fileRepository.findByPostIdAndDeletedAtIsNull(postId)).willReturn(List.of(oldFile));
        given(fileRepository.findByIdAndDeletedAtIsNull(oldFile.getId())).willReturn(Optional.of(oldFile));
        given(fileRepository.findAllByIdInAndDeletedAtIsNull(updatedFileIds)).willReturn(List.of(newFile));

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(oldFilePath)).thenReturn(true);
            mockedFiles.when(() -> Files.exists(newFilePath)).thenReturn(true);

            // when
            fileService.updateFiles(userId, postId, updatedFileIds);
        }

        assertThat(oldFile.getDeletedAt()).isNotNull();
        assertThat(newFile.getPostId()).isEqualTo(postId);
    }


    // ========== 파일 삭제 ==========
    @Test
    @DisplayName("파일 삭제 성공 - 물리 파일 존재함")
    void delete_success_physicalFileExists() {
        // given
        Long userId = 1L;

        File file = File.register(userId, "테스트 파일.txt", "uploads/2026/09/18/uuid", 1000L, "text/plain");
        ReflectionTestUtils.setField(file, "id", 1L);

        Path path = Paths.get(file.getStoredPath());

        given(fileRepository.findByIdAndDeletedAtIsNull(file.getId())).willReturn(Optional.of(file));

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(path)).thenReturn(true);

            // when
            fileService.delete(userId, file.getId());

            // then
            mockedFiles.verify(() -> Files.delete(path));
            assertThat(file.getDeletedAt()).isNotNull();
        }
    }

    @Test
    @DisplayName("파일 삭제 성공 - 물리 파일 없음")
    void delete_success_physicalFileNotExists() {
        // given
        Long userId = 1L;

        File file = File.register(userId, "테스트 파일.txt", "uploads/2026/09/18/uuid", 1000L, "text/plain");
        ReflectionTestUtils.setField(file, "id", 1L);

        Path path = Paths.get(file.getStoredPath());

        given(fileRepository.findByIdAndDeletedAtIsNull(file.getId())).willReturn(Optional.of(file));

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(path)).thenReturn(false);

            // when
            fileService.delete(userId, file.getId());

            // then
            mockedFiles.verify(() -> Files.delete(path), never());
            assertThat(file.getDeletedAt()).isNotNull();
        }
    }

    @Test
    @DisplayName("파일 삭제 성공 - 물리 파일 삭제 중 IOException 발생")
    void delete_success_ioException() {
        // given
        Long userId = 1L;

        File file = File.register(userId, "테스트 파일.txt", "uploads/2026/09/18/uuid", 1000L, "text/plain");
        ReflectionTestUtils.setField(file, "id", 1L);

        Path path = Paths.get(file.getStoredPath());

        given(fileRepository.findByIdAndDeletedAtIsNull(file.getId())).willReturn(Optional.of(file));

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(path)).thenReturn(true);
            mockedFiles.when(() -> Files.delete(path)).thenThrow(IOException.class);

            // when
            fileService.delete(userId, file.getId());
        }

        // then
        assertThat(file.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("파일 삭제 실패 - 파일 없음")
    void delete_fail_fileNotFound() {
        // given
        Long fileId = 99L;

        given(fileRepository.findByIdAndDeletedAtIsNull(fileId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> fileService.delete(1L, fileId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(FileExceptionEnum.FILE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("파일 삭제 실패 - 소유자 아님")
    void delete_fail_forbidden() {
        // given
        Long userId = 1L;

        File file = File.register(2L, "테스트 파일.txt", "uploads/2026/09/18/uuid", 1000L, "text/plain");
        ReflectionTestUtils.setField(file, "id", 1L);

        given(fileRepository.findByIdAndDeletedAtIsNull(file.getId())).willReturn(Optional.of(file));

        // when & then
        assertThatThrownBy(() -> fileService.delete(userId, file.getId()))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(FileExceptionEnum.FILE_FORBIDDEN.getMessage());
    }
}