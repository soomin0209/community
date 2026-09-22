package com.community.domain.comment.controller;

import com.community.common.config.security.CustomUserDetails;
import com.community.common.config.web.SuspendedCheckInterceptor;
import com.community.common.config.web.UserVisitInterceptor;
import com.community.common.config.web.WebMvcConfig;
import com.community.common.dto.CursorResponse;
import com.community.common.dto.PageResponse;
import com.community.domain.comment.dto.request.CreateCommentRequest;
import com.community.domain.comment.dto.request.UpdateCommentRequest;
import com.community.domain.comment.dto.response.CreateCommentResponse;
import com.community.domain.comment.dto.response.GetAllCommentsResponse;
import com.community.domain.comment.dto.response.GetMyCommentsResponse;
import com.community.domain.comment.dto.response.UpdateCommentResponse;
import com.community.domain.comment.service.CommentService;
import com.community.domain.user.enums.UserRole;
import org.junit.jupiter.api.AfterEach;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CommentController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {WebMvcConfig.class, SuspendedCheckInterceptor.class, UserVisitInterceptor.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }


    // ========== 댓글 등록 ==========
    @Test
    @DisplayName("댓글 등록 성공")
    void create_success() throws Exception {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(1L, UserRole.BRONZE.name());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        CreateCommentRequest request = new CreateCommentRequest(null, "댓글 테스트");
        CreateCommentResponse response = new CreateCommentResponse(1L, null, "댓글 테스트", LocalDateTime.now());

        given(commentService.create(eq(1L), eq(1L), any(CreateCommentRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/posts/{postId}/comments", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.parentId").doesNotExist())
                .andExpect(jsonPath("$.data.content").value("댓글 테스트"));
    }

    @Test
    @DisplayName("댓글 등록 실패 - 부모 식별자 양수 아님")
    void create_fail_parentIdIsNotPositive() throws Exception {
        // given
        CreateCommentRequest request = new CreateCommentRequest(-1L, "댓글 테스트");

        // when & then
        mockMvc.perform(post("/api/posts/{postId}/comments", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("부모 식별자는 1 이상이어야 합니다"));
    }

    @Test
    @DisplayName("댓글 등록 실패 - 댓글 공백")
    void create_fail_contentIsNull() throws Exception {
        // given
        CreateCommentRequest request = new CreateCommentRequest(null, null);

        // when & then
        mockMvc.perform(post("/api/posts/{postId}/comments", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("댓글을 입력해주세요"));
    }

    @Test
    @DisplayName("댓글 등록 실패 - 댓글 형식 불일치")
    void create_fail_invalidContent() throws Exception {
        // given
        CreateCommentRequest request = new CreateCommentRequest(null, "a".repeat(201));

        // when & then
        mockMvc.perform(post("/api/posts/{postId}/comments", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("댓글은 200자 이하여야 합니다"));
    }


    // ========== 댓글 목록 조회 ==========
    @Test
    @DisplayName("댓글 목록 조회 성공")
    void getAll_success() throws Exception {
        // given
        CursorResponse<GetAllCommentsResponse> response = new CursorResponse<>(
                List.of(
                        new GetAllCommentsResponse(1L, null, "브론즈유저", false, "맞아요! 산책하기 딱 좋은 날씨네요 ㅎㅎ", LocalDateTime.now(), 0),
                        new GetAllCommentsResponse(2L, 1L, "실버유저", true, "저도 나가볼까 봐요!", LocalDateTime.now(), 1)
                ), null, false, 10
        );

        given(commentService.getAll(eq(1L), any(), isNull())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/posts/{postId}/comments", 1L)
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist())
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @Test
    @DisplayName("댓글 목록 조회 실패 - 페이지 크기 양수 아님")
    void getAll_fail_sizeIsNotPositive() throws Exception {
        // when & then
        mockMvc.perform(get("/api/posts/{postId}/comments", 1L)
                        .param("size", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지 크기는 1 이상이어야 합니다"));
    }

    @Test
    @DisplayName("댓글 목록 조회 실패 - 페이지 크기 100 초과")
    void getAll_fail_sizeExceed100() throws Exception {
        // when & then
        mockMvc.perform(get("/api/posts/{postId}/comments", 1L)
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지 크기는 100 이하여야 합니다"));
    }


    // ========== 내 댓글 목록 조회 ==========
    @Test
    @DisplayName("내 댓글 목록 조회 성공")
    void getMine_success() throws Exception {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(1L, UserRole.BRONZE.name());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        PageResponse<GetMyCommentsResponse> response = new PageResponse<>(
                List.of(
                        new GetMyCommentsResponse(1L, 1L, "오늘 날씨 정말 좋네요!", "맞아요! 산책하기 딱 좋은 날씨네요 ㅎㅎ", LocalDateTime.now()),
                        new GetMyCommentsResponse(2L, 2L, "점심 메뉴 추천해주세요", "제육볶음 어떠세요?", LocalDateTime.now())
                ), 0, 1, 2, 20, true
        );

        given(commentService.getMine(eq(1L), any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/comments/my")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.isLast").value(true));
    }

    @Test
    @DisplayName("내 댓글 목록 조회 실패 - 페이지 음수")
    void getMine_fail_pageIsNegative() throws Exception {
        // when & then
        mockMvc.perform(get("/api/comments/my")
                        .param("page", "-1")
                        .param("size", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지는 0 이상이어야 합니다"));
    }

    @Test
    @DisplayName("내 댓글 목록 조회 실패 - 페이지 크기 양수 아님")
    void getMine_fail_sizeIsNotPositive() throws Exception {
        // when & then
        mockMvc.perform(get("/api/comments/my")
                        .param("page", "0")
                        .param("size", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지 크기는 1 이상이어야 합니다"));
    }

    @Test
    @DisplayName("내 댓글 목록 조회 실패 - 페이지 크기 100 초과")
    void getMine_fail_sizeExceed100() throws Exception {
        // when & then
        mockMvc.perform(get("/api/comments/my")
                        .param("page", "0")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지 크기는 100 이하여야 합니다"));
    }


    // ========== 댓글 수정 ==========
    @Test
    @DisplayName("댓글 수정 성공")
    void update_success() throws Exception {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(1L, UserRole.BRONZE.name());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        UpdateCommentRequest request = new UpdateCommentRequest("댓글 수정");
        UpdateCommentResponse response = new UpdateCommentResponse(1L, "댓글 수정", LocalDateTime.now(), LocalDateTime.now());

        given(commentService.update(eq(1L), eq(1L), any(UpdateCommentRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/comments/{commentId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("댓글 수정"));
    }

    @Test
    @DisplayName("댓글 수정 실패 - 수정할 내용 없음")
    void update_fail_updateNoContent() throws Exception {
        // given
        UpdateCommentRequest request = new UpdateCommentRequest(null);

        // when & then
        mockMvc.perform(patch("/api/comments/{commentId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("수정할 내용이 없습니다"));
    }

    @Test
    @DisplayName("댓글 수정 실패 - 댓글 형식 불일치")
    void update_fail_invalidContent() throws Exception {
        // given
        UpdateCommentRequest request = new UpdateCommentRequest("a".repeat(201));

        // when & then
        mockMvc.perform(patch("/api/comments/{commentId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("댓글은 200자 이하여야 합니다"));
    }


    // ========== 댓글 삭제 ==========
    @Test
    @DisplayName("댓글 삭제 성공")
    void delete_success() throws Exception {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(1L, UserRole.BRONZE.name());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        // when & then
        mockMvc.perform(delete("/api/comments/{commentId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(commentService).delete(1L, 1L);
    }
}