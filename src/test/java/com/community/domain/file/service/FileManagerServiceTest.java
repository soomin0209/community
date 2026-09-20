package com.community.domain.file.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.file.entity.File;
import com.community.domain.file.exception.FileExceptionEnum;
import com.community.domain.file.repository.FileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class FileManagerServiceTest {

    @InjectMocks
    private FileManagerService fileManagerService;

    @Mock
    private FileRepository fileRepository;


    // ========== 파일 강제 삭제 ==========
    @Test
    @DisplayName("파일 강제 삭제 성공 - 물리 파일 존재함")
    void delete_success_physicalFileExists() {
        // given
        Long userId = 1L;

        File file = File.register(2L, "테스트 파일.txt", "uploads/2026/09/18/uuid1", 1000L, "text/plain");
        ReflectionTestUtils.setField(file, "id", 1L);

        Path path = Paths.get(file.getStoredPath());

        given(fileRepository.findByIdAndDeletedAtIsNull(file.getId())).willReturn(Optional.of(file));

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(path)).thenReturn(true);

            // when
            fileManagerService.delete(userId, file.getId());

            // then
            mockedFiles.verify(() -> Files.delete(path));
            assertThat(file.getDeletedAt()).isNotNull();
        }
    }

    @Test
    @DisplayName("파일 강제 삭제 성공 - 물리 파일 없음")
    void delete_success_physicalFileNotExists() {
        // given
        Long userId = 1L;

        File file = File.register(2L, "테스트 파일.txt", "uploads/2026/09/18/uuid1", 1000L, "text/plain");
        ReflectionTestUtils.setField(file, "id", 1L);

        Path path = Paths.get(file.getStoredPath());

        given(fileRepository.findByIdAndDeletedAtIsNull(file.getId())).willReturn(Optional.of(file));

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(path)).thenReturn(false);

            // when
            fileManagerService.delete(userId, file.getId());

            // then
            mockedFiles.verify(() -> Files.delete(path), never());
            assertThat(file.getDeletedAt()).isNotNull();
        }
    }

    @Test
    @DisplayName("파일 강제 삭제 성공 - 물리 파일 삭제 중 IOException 발생")
    void delete_success_ioException() {
        // given
        Long userId = 1L;

        File file = File.register(2L, "테스트 파일.txt", "uploads/2026/09/18/uuid1", 1000L, "text/plain");
        ReflectionTestUtils.setField(file, "id", 1L);

        Path path = Paths.get(file.getStoredPath());

        given(fileRepository.findByIdAndDeletedAtIsNull(file.getId())).willReturn(Optional.of(file));

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(path)).thenReturn(true);
            mockedFiles.when(() -> Files.delete(path)).thenThrow(IOException.class);

            // when
            fileManagerService.delete(userId, file.getId());
        }

        // then
        assertThat(file.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("파일 강제 삭제 실패 - 파일 없음")
    void delete_fail_fileNotFound() {
        // given
        Long fileId = 99L;

        given(fileRepository.findByIdAndDeletedAtIsNull(fileId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> fileManagerService.delete(1L, fileId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(FileExceptionEnum.FILE_NOT_FOUND.getMessage());
    }
}