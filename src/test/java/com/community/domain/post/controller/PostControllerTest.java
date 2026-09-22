package com.community.domain.post.controller;

import com.community.common.config.security.CustomUserDetails;
import com.community.common.config.web.SuspendedCheckInterceptor;
import com.community.common.config.web.UserVisitInterceptor;
import com.community.common.config.web.WebMvcConfig;
import com.community.common.dto.PageResponse;
import com.community.domain.post.dto.request.CreatePostRequest;
import com.community.domain.post.dto.request.UpdatePostRequest;
import com.community.domain.post.dto.response.*;
import com.community.domain.post.enums.PostType;
import com.community.domain.post.service.PostService;
import com.community.domain.post.service.PostViewService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PostController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {WebMvcConfig.class, SuspendedCheckInterceptor.class, UserVisitInterceptor.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PostService postService;

    @MockitoBean
    private PostViewService postViewService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }


    // ========== 게시물 등록 ==========
    @Test
    @DisplayName("게시물 등록 성공")
    void create_success() throws Exception {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(1L, UserRole.BRONZE.name());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        CreatePostRequest request = new CreatePostRequest(1L, "테스트 게시물", "테스트 게시물입니다", PostType.GENERAL, null);
        CreatePostResponse response = new CreatePostResponse(1L, 1L, "테스트 게시물", "테스트 게시물입니다", "브론즈유저", PostType.GENERAL, LocalDateTime.now(), null);

        given(postService.create(eq(1L), any(CreatePostRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.boardId").value(1L))
                .andExpect(jsonPath("$.data.title").value("테스트 게시물"))
                .andExpect(jsonPath("$.data.content").value("테스트 게시물입니다"))
                .andExpect(jsonPath("$.data.nickname").value("브론즈유저"))
                .andExpect(jsonPath("$.data.type").value(PostType.GENERAL.name()))
                .andExpect(jsonPath("$.data.files").doesNotExist());
    }

    @Test
    @DisplayName("게시물 등록 실패 - 게시판 아이디 공백")
    void create_fail_boardIdIsNull() throws Exception {
        // given
        CreatePostRequest request = new CreatePostRequest(null, "테스트 게시물", "테스트 게시물입니다", PostType.GENERAL, null);

        // when & then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("게시판 아이디를 입력해주세요"));
    }

    @Test
    @DisplayName("게시물 등록 실패 - 게시판 아이디 형식 불일치")
    void create_fail_invalidBoardId() throws Exception {
        // given
        CreatePostRequest request = new CreatePostRequest(-1L, "테스트 게시물", "테스트 게시물입니다", PostType.GENERAL, null);

        // when & then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("게시판 아이디는 1 이상이어야 합니다"));
    }

    @Test
    @DisplayName("게시물 등록 실패 - 제목 공백")
    void create_fail_titleIsNull() throws Exception {
        // given
        CreatePostRequest request = new CreatePostRequest(1L, null, "테스트 게시물입니다", PostType.GENERAL, null);

        // when & then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("제목을 입력해주세요"));
    }

    @Test
    @DisplayName("게시물 등록 실패 - 제목 형식 불일치")
    void create_fail_titleExceed50() throws Exception {
        // given
        CreatePostRequest request = new CreatePostRequest(1L, "a".repeat(51), "테스트 게시물입니다", PostType.GENERAL, null);

        // when & then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("제목은 50자 이하여야 합니다"));
    }

    @Test
    @DisplayName("게시물 등록 실패 - 제목 형식 불일치")
    void create_fail_contentIsNull() throws Exception {
        // given
        CreatePostRequest request = new CreatePostRequest(1L, "테스트 게시물", null, PostType.GENERAL, null);

        // when & then
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("내용을 입력해주세요"));
    }


    // ========== 게시물 단건 조회 ==========
    @Test
    @DisplayName("게시물 단건 조회 성공")
    void getOne_success() throws Exception {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(1L, UserRole.BRONZE.name());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        GetOnePostResponse response = new GetOnePostResponse(1L, 1L, "테스트 게시물", "테스트 게시물입니다",
                "브론즈유저", PostType.GENERAL, LocalDateTime.now(), LocalDateTime.now(), 0L, 0L, 0L, 0L, null);

        given(postService.getOne(eq(1L), any(), eq(1L))).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/posts/{postId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.boardId").value(1L))
                .andExpect(jsonPath("$.data.title").value("테스트 게시물"))
                .andExpect(jsonPath("$.data.content").value("테스트 게시물입니다"))
                .andExpect(jsonPath("$.data.nickname").value("브론즈유저"))
                .andExpect(jsonPath("$.data.type").value(PostType.GENERAL.name()));
    }


    // ========== 게시물 목록 조회 ==========
    @Test
    @DisplayName("게시물 목록 조회 성공")
    void getAll_success() throws Exception {
        // given
        PageResponse<GetAllPostsResponse> response = new PageResponse<>(
                List.of(
                        new GetAllPostsResponse(1L, 1L, "테스트 게시물1", "브론즈유저", PostType.GENERAL,
                                true, LocalDateTime.now(), 0L, 0L),
                        new GetAllPostsResponse(2L, 1L, "테스트 게시물1", "실버유저", PostType.GENERAL,
                                false, LocalDateTime.now(), 0L, 0L)
                ), 0, 1, 2, 20, true
        );

        given(postService.getAll(any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/posts")
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
    @DisplayName("게시물 목록 조회 실패 - 페이지 음수")
    void getAll_fail_pageIsNegative() throws Exception {
        // when & then
        mockMvc.perform(get("/api/posts")
                        .param("page", "-1")
                        .param("size", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지는 0 이상이어야 합니다"));
    }

    @Test
    @DisplayName("게시물 목록 조회 실패 - 페이지 양수 아님")
    void getAll_fail_sizeIsNotPositive() throws Exception {
        // when & then
        mockMvc.perform(get("/api/posts")
                        .param("page", "0")
                        .param("size", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지 크기는 1 이상이어야 합니다"));
    }

    @Test
    @DisplayName("게시물 목록 조회 실패 - 페이지 크기 100 초과")
    void getAll_fail_sizeExceed100() throws Exception {
        // when & then
        mockMvc.perform(get("/api/posts")
                        .param("page", "0")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지 크기는 100 이하여야 합니다"));
    }


    // ========== 내 게시물 목록 조회 ==========
    @Test
    @DisplayName("내 게시물 목록 조회 성공")
    void getMine_success() throws Exception {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(1L, UserRole.BRONZE.name());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        PageResponse<GetAllPostsResponse> response = new PageResponse<>(
                List.of(
                        new GetAllPostsResponse(1L, 1L, "테스트 게시물1", "브론즈유저", PostType.GENERAL,
                                true, LocalDateTime.now(), 0L, 0L),
                        new GetAllPostsResponse(2L, 1L, "테스트 게시물1", "브론즈유저", PostType.QUESTION,
                                false, LocalDateTime.now(), 0L, 0L)
                ), 0, 1, 2, 20, true
        );

        given(postService.getMine(eq(1L), any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/posts/my")
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
    @DisplayName("내 게시물 목록 조회 실패 - 페이지 음수")
    void getMine_fail_pageIsNegative() throws Exception {
        // when & then
        mockMvc.perform(get("/api/posts/my")
                        .param("page", "-1")
                        .param("size", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지는 0 이상이어야 합니다"));
    }

    @Test
    @DisplayName("내 게시물 목록 조회 실패 - 페이지 양수 아님")
    void getMine_fail_sizeIsNotPositive() throws Exception {
        // when & then
        mockMvc.perform(get("/api/posts/my")
                        .param("page", "0")
                        .param("size", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지 크기는 1 이상이어야 합니다"));
    }

    @Test
    @DisplayName("내 게시물 목록 조회 실패 - 페이지 크기 100 초과")
    void getMine_fail_sizeExceed100() throws Exception {
        // when & then
        mockMvc.perform(get("/api/posts/my")
                        .param("page", "0")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("페이지 크기는 100 이하여야 합니다"));
    }


    // ========== 주간 인기 게시물 목록 조회 ==========
    @Test
    @DisplayName("주간 인기 게시물 목록 조회 성공")
    void getBest_success() throws Exception {
        // given
        List<GetBestPostsResponse> response = List.of(
                new GetBestPostsResponse(1L, "테스트 게시물1", 50L, 500L),
                new GetBestPostsResponse(2L, "테스트 게시물2", 40L, 400L),
                new GetBestPostsResponse(3L, "테스트 게시물3", 30L, 300L),
                new GetBestPostsResponse(4L, "테스트 게시물4", 20L, 200L),
                new GetBestPostsResponse(5L, "테스트 게시물5", 10L, 100L)
        );

        given(postViewService.getWeeklyBestPosts()).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/posts/best"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].title").value("테스트 게시물1"))
                .andExpect(jsonPath("$.data[0].weeklyViewCount").value(50L))
                .andExpect(jsonPath("$.data[0].totalViewCount").value(500L));
    }


    // ========== 게시물 수정 ==========
    @Test
    @DisplayName("게시물 수정 성공")
    void update_success() throws Exception {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(1L, UserRole.BRONZE.name());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        UpdatePostRequest request = new UpdatePostRequest(1L, "테스트 게시물", "테스트 게시물입니다", null);
        UpdatePostResponse response = new UpdatePostResponse(1L, 1L, "테스트 게시물", "테스트 게시물입니다",
                "브론즈유저", LocalDateTime.now(), LocalDateTime.now());

        given(postService.update(eq(1L), eq(1L), any(UpdatePostRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/posts/{postId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.boardId").value(1L))
                .andExpect(jsonPath("$.data.title").value("테스트 게시물"))
                .andExpect(jsonPath("$.data.content").value("테스트 게시물입니다"));
    }

    @Test
    @DisplayName("게시물 수정 실패 - 제목 형식 불일치")
    void update_fail_invalidTitle() throws Exception {
        // given
        UpdatePostRequest request = new UpdatePostRequest(1L, "a".repeat(51), "테스트 게시물입니다", null);

        // when & then
        mockMvc.perform(patch("/api/posts/{postId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("제목은 1~50자여야 합니다"));
    }

    @Test
    @DisplayName("게시물 수정 실패 - 내용 형식 불일치")
    void update_fail_invalidContent() throws Exception {
        // given
        UpdatePostRequest request = new UpdatePostRequest(1L, "테스트 게시물", "", null);

        // when & then
        mockMvc.perform(patch("/api/posts/{postId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("내용은 최소 1자 이상이어야 합니다"));
    }


    // ========== 게시물 삭제 ==========
    @Test
    @DisplayName("게시물 삭제 성공")
    void delete_success() throws Exception {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(1L, UserRole.BRONZE.name());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        // when & then
        mockMvc.perform(delete("/api/posts/{postId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(postService).delete(1L, 1L);
    }
}