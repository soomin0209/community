package com.community.domain.file.controller;

import com.community.common.config.security.CustomUserDetails;
import com.community.common.config.web.SuspendedCheckInterceptor;
import com.community.common.config.web.UserVisitInterceptor;
import com.community.common.config.web.WebMvcConfig;
import com.community.common.exception.GlobalExceptionHandler;
import com.community.domain.file.dto.response.DownloadFileResponse;
import com.community.domain.file.dto.response.UploadFileResponse;
import com.community.domain.file.service.FileService;
import com.community.domain.user.enums.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = {FileController.class, GlobalExceptionHandler.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {WebMvcConfig.class, SuspendedCheckInterceptor.class, UserVisitInterceptor.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileService fileService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }


    // ========== 파일 업로드 ==========
    @Test
    @DisplayName("파일 업로드 성공")
    void upload_success() throws Exception {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(1L, UserRole.BRONZE.name());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        MockMultipartFile file1 = new MockMultipartFile("files", "테스트 파일1.txt", "text/plain", "테스트 파일 내용".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "테스트 파일2.txt", "text/plain", "테스트 파일 내용".getBytes());

        List<UploadFileResponse> response = List.of(
                new UploadFileResponse(1L, "uploads/2026/09/21/uuid1", "테스트 파일1.txt", 1000L, "text/plain", LocalDateTime.now()),
                new UploadFileResponse(2L, "uploads/2026/09/21/uuid2", "테스트 파일2.txt", 2000L, "text/plain", LocalDateTime.now())
        );

        given(fileService.upload(eq(1L), anyList())).willReturn(response);

        // when & then
        mockMvc.perform(multipart("/api/files/upload")
                .file(file1)
                .file(file2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].originalFilename").value("테스트 파일1.txt"))
                .andExpect(jsonPath("$.data[1].originalFilename").value("테스트 파일2.txt"));
    }


    // ========== 파일 다운로드 ==========
    @Test
    @DisplayName("파일 다운로드 성공")
    void download_success() throws Exception {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(1L, UserRole.BRONZE.name());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        DownloadFileResponse response = new DownloadFileResponse(
                new ByteArrayResource("테스트 파일 내용".getBytes()), "테스트 파일.txt", 1000L, "text/plain");

        given(fileService.download(eq(1L), eq(1L))).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/files/download/{fileId}", 1L))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/plain"))
                .andExpect(header().string("Content-Disposition", "attachment; filename*=UTF-8''"
                        + UriUtils.encode("테스트 파일.txt", StandardCharsets.UTF_8)))
                .andExpect(content().bytes("테스트 파일 내용".getBytes()));
    }


    // ========== 파일 삭제 ==========
    @Test
    @DisplayName("파일 삭제 성공")
    void delete_success() throws Exception {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(1L, UserRole.BRONZE.name());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        // when & then
        mockMvc.perform(delete("/api/files/{fileId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(fileService).delete(1L, 1L);
    }
}