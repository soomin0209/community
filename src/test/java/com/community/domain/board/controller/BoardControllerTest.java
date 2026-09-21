package com.community.domain.board.controller;

import com.community.common.config.web.SuspendedCheckInterceptor;
import com.community.common.config.web.UserVisitInterceptor;
import com.community.common.config.web.WebMvcConfig;
import com.community.common.exception.GlobalExceptionHandler;
import com.community.domain.board.dto.response.GetAllBoardResponse;
import com.community.domain.board.service.BoardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {BoardController.class, GlobalExceptionHandler.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {WebMvcConfig.class, SuspendedCheckInterceptor.class, UserVisitInterceptor.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
class BoardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BoardService boardService;


    // ========== 게시판 목록 조회 ==========
    @Test
    @DisplayName("게시판 목록 조회 성공")
    void getAll_success() throws Exception {
        // given
        GetAllBoardResponse response = new GetAllBoardResponse(1L, "자유게시판");

        given(boardService.getAll()).willReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/api/boards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("자유게시판"));
    }
}