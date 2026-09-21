package com.community.domain.board.controller;

import com.community.common.config.web.SuspendedCheckInterceptor;
import com.community.common.config.web.UserVisitInterceptor;
import com.community.common.config.web.WebMvcConfig;
import com.community.common.exception.GlobalExceptionHandler;
import com.community.domain.board.dto.request.CreateBoardRequest;
import com.community.domain.board.dto.request.UpdateBoardRequest;
import com.community.domain.board.dto.response.CreateBoardResponse;
import com.community.domain.board.dto.response.UpdateBoardResponse;
import com.community.domain.board.service.BoardAdminService;
import com.community.domain.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = BoardAdminController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {WebMvcConfig.class, SuspendedCheckInterceptor.class, UserVisitInterceptor.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
class BoardAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BoardAdminService boardAdminService;


    // ========== 게시판 등록 ==========
    @Test
    @DisplayName("게시판 등록 성공")
    void create_success() throws Exception {
        // given
        CreateBoardRequest request = new CreateBoardRequest("자유게시판", UserRole.BRONZE);
        CreateBoardResponse response = new CreateBoardResponse(1L, "자유게시판", UserRole.BRONZE, LocalDateTime.now());

        given(boardAdminService.create(any(CreateBoardRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/admin/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("자유게시판"))
                .andExpect(jsonPath("$.data.minRole").value(UserRole.BRONZE.name()));
    }

    @Test
    @DisplayName("게시판 등록 실패 - 게시판 이름 공백")
    void create_fail_nameIsNull() throws Exception {
        // given
        CreateBoardRequest request = new CreateBoardRequest(null, UserRole.BRONZE);

        // when & then
        mockMvc.perform(post("/api/admin/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("게시판 이름을 입력해주세요"));
    }

    @Test
    @DisplayName("게시판 등록 실패 - 게시판 이름 형식 불일치")
    void create_fail_invalidName() throws Exception {
        // given
        CreateBoardRequest request = new CreateBoardRequest("자유게시판자유게시판자유게시판자유게시판자유", UserRole.BRONZE);

        // when & then
        mockMvc.perform(post("/api/admin/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("게시판 이름은 20자 이하여야 합니다"));
    }


    // ========== 게시판 수정 ==========
    @Test
    @DisplayName("게시판 수정 성공")
    void update_success() throws Exception {
        // given
        UpdateBoardRequest request = new UpdateBoardRequest("질문게시판",  UserRole.BRONZE);
        UpdateBoardResponse response = new UpdateBoardResponse(1L, "질문게시판", UserRole.BRONZE, LocalDateTime.now(), LocalDateTime.now());

        given(boardAdminService.update(eq(1L), any(UpdateBoardRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/admin/boards/{boardId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("질문게시판"))
                .andExpect(jsonPath("$.data.minRole").value(UserRole.BRONZE.name()));
    }

    @Test
    @DisplayName("게시판 수정 실패 - 게시판 이름 형식 불일치")
    void update_fail_invalidName() throws Exception {
        // given
        UpdateBoardRequest request = new UpdateBoardRequest("질문게시판질문게시판질문게시판질문게시판질문", UserRole.BRONZE);

        // when & then
        mockMvc.perform(patch("/api/admin/boards/{boardId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("게시판 이름은 20자 이하여야 합니다"));
    }


    // ========== 게시판 삭제 ==========
    @Test
    @DisplayName("게시판 삭제 성공")
    void delete_success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/admin/boards/{boardId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(boardAdminService).delete(1L);
    }
}