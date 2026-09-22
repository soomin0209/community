package com.community.domain.post.controller;

import com.community.common.config.security.CustomUserDetails;
import com.community.common.config.web.SuspendedCheckInterceptor;
import com.community.common.config.web.UserVisitInterceptor;
import com.community.common.config.web.WebMvcConfig;
import com.community.domain.post.dto.request.MovePostRequest;
import com.community.domain.post.dto.response.MovePostResponse;
import com.community.domain.post.dto.response.PinPostResponse;
import com.community.domain.post.enums.PostType;
import com.community.domain.post.service.PostManagerService;
import com.community.domain.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PostManagerController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {WebMvcConfig.class, SuspendedCheckInterceptor.class, UserVisitInterceptor.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
class PostManagerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PostManagerService postManagerService;


    // ========== 게시물 고정/해제 ==========
    @Test
    @DisplayName("게시물 고정 성공")
    void pin_success() throws Exception {
        // given
        PinPostResponse response = new PinPostResponse(1L, "테스트 게시물", "브론즈유저", PostType.GENERAL,
                LocalDateTime.now(), true, LocalDateTime.now());

        given(postManagerService.pin(eq(1L))).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/manager/posts/{postId}/pin", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("테스트 게시물"))
                .andExpect(jsonPath("$.data.nickname").value("브론즈유저"))
                .andExpect(jsonPath("$.data.type").value(PostType.GENERAL.name()))
                .andExpect(jsonPath("$.data.isPinned").value(true));
    }


    // ========== 게시물 강제 이동 ==========
    @Test
    @DisplayName("게시물 강제 이동 성공")
    void move_success() throws Exception {
        // given
        MovePostRequest request = new MovePostRequest(1L);
        MovePostResponse response = new MovePostResponse(1L, 1L, "테스트 게시물", "브론즈유저", LocalDateTime.now(), LocalDateTime.now());

        given(postManagerService.move(eq(1L), any(MovePostRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/manager/posts/{postId}/move", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.boardId").value(1L))
                .andExpect(jsonPath("$.data.title").value("테스트 게시물"))
                .andExpect(jsonPath("$.data.nickname").value("브론즈유저"));
    }

    @Test
    @DisplayName("게시물 강제 이동 실패 - 변경할 게시판 아이디 공백")
    void move_fail_boardIdIsNull() throws Exception {
        // given
        MovePostRequest request = new MovePostRequest(null);

        // when & then
        mockMvc.perform(patch("/api/manager/posts/{postId}/move", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("변경할 게시판 아이디를 입력해주세요"));
    }


    // ========== 게시물 강제 삭제 ==========
    @Test
    @DisplayName("게시물 강제 삭제 성공")
    void delete_success() throws Exception {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(1L, UserRole.BRONZE.name());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        // when & then
        mockMvc.perform(delete("/api/manager/posts/{postId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(postManagerService).delete(1L, 1L);
    }
}