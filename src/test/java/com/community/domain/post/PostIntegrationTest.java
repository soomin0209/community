package com.community.domain.post;

import com.community.domain.auth.dto.request.LoginRequest;
import com.community.domain.auth.dto.request.UserSignupRequest;
import com.community.domain.board.entity.Board;
import com.community.domain.board.exception.BoardExceptionEnum;
import com.community.domain.board.repository.BoardRepository;
import com.community.domain.comment.entity.Comment;
import com.community.domain.comment.repository.CommentRepository;
import com.community.domain.post.entity.Post;
import com.community.domain.post.enums.PostType;
import com.community.domain.post.repository.PostRepository;
import com.community.domain.user.entity.User;
import com.community.domain.user.enums.UserRole;
import com.community.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PostIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    private static final String MANAGER_LOGIN_ID = "manager123";
    private static final String USER_LOGIN_ID = "user123";
    private static final String WRITER_LOGIN_ID = "writer123";
    private static final String PASSWORD = "password123@";
    private static final String BOARD_NAME = "자유게시판";
    private static final String POST_TITLE = "게시물 제목";
    private static final String POST_CONTENT = "게시물 내용";
    private static final String COMMENT_CONTENT = "댓글 내용";

    @AfterEach
    void cleanUp() {
        List.of(MANAGER_LOGIN_ID, USER_LOGIN_ID, WRITER_LOGIN_ID).forEach(loginId ->
                userRepository.findByLoginIdAndDeletedAtIsNull(loginId)
                        .ifPresent(u -> userRepository.delete(u)));
    }


    // ========== 실제 Security 필터 체인 (GET 공개 / 내 게시물 인증) ==========
    @Test
    @DisplayName("비로그인 사용자도 공개 게시판의 게시물 조회 성공")
    void getOne_success_anonymous() throws Exception {
        // given
        signup(WRITER_LOGIN_ID);
        Long writerId = getUserId(WRITER_LOGIN_ID);

        Board board = boardRepository.save(Board.register(BOARD_NAME, null));
        Post post = postRepository.save(Post.register(writerId, board.getId(), POST_TITLE, POST_CONTENT, PostType.GENERAL));

        // when & then
        mockMvc.perform(get("/api/posts/{postId}", post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value(POST_TITLE));
    }

    @Test
    @DisplayName("인증 없이 내 게시물 목록 조회 시도 - 401")
    void getMine_fail_noToken_unauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/api/posts/my"))
                .andExpect(status().isUnauthorized());
    }


    // ========== 실제 Security 필터 체인 + ROLE_MANAGER 접근 제어 ==========
    @Test
    @DisplayName("매니저 권한으로 게시물 고정 성공")
    void pin_success_byManager() throws Exception {
        // given
        signup(WRITER_LOGIN_ID);
        Long writerId = getUserId(WRITER_LOGIN_ID);

        Board board = boardRepository.save(Board.register(BOARD_NAME, null));
        Post post = postRepository.save(Post.register(writerId, board.getId(), POST_TITLE, POST_CONTENT, PostType.GENERAL));

        signup(MANAGER_LOGIN_ID);
        String accessToken = getAccessToken(MANAGER_LOGIN_ID);

        // when & then
        mockMvc.perform(patch("/api/manager/posts/{postId}/pin", post.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPinned").value(true));
    }

    @Test
    @DisplayName("일반 사용자가 게시물 고정 시도 - 403")
    void pin_fail_byGeneralUser_forbidden() throws Exception {
        // given
        signup(WRITER_LOGIN_ID);
        Long writerId = getUserId(WRITER_LOGIN_ID);

        Board board = boardRepository.save(Board.register(BOARD_NAME, null));
        Post post = postRepository.save(Post.register(writerId, board.getId(), POST_TITLE, POST_CONTENT, PostType.GENERAL));

        signup(USER_LOGIN_ID);
        String accessToken = getAccessToken(USER_LOGIN_ID);

        // when & then
        mockMvc.perform(patch("/api/manager/posts/{postId}/pin", post.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증 없이 게시물 고정 시도 - 401")
    void pin_fail_noToken_unauthorized() throws Exception {
        // given
        signup(WRITER_LOGIN_ID);
        Long writerId = getUserId(WRITER_LOGIN_ID);

        Board board = boardRepository.save(Board.register(BOARD_NAME, null));
        Post post = postRepository.save(Post.register(writerId, board.getId(), POST_TITLE, POST_CONTENT, PostType.GENERAL));

        // when & then
        mockMvc.perform(patch("/api/manager/posts/{postId}/pin", post.getId()))
                .andExpect(status().isUnauthorized());
    }


    // ========== 실제 게시판 권한 검증 + 작성자 우회 ==========
    @Test
    @DisplayName("접근 권한이 없는 사용자의 게시물 조회 시도 - 403")
    void getOne_fail_boardAccessDenied() throws Exception {
        // given
        signup(WRITER_LOGIN_ID);
        Long writerId = getUserId(WRITER_LOGIN_ID);

        Board board = boardRepository.save(Board.register(BOARD_NAME, UserRole.SILVER));
        Post post = postRepository.save(Post.register(writerId, board.getId(), POST_TITLE, POST_CONTENT, PostType.GENERAL));

        signup(USER_LOGIN_ID);
        String accessToken = getAccessToken(USER_LOGIN_ID);

        // when & then
        mockMvc.perform(get("/api/posts/{postId}", post.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(BoardExceptionEnum.BOARD_ACCESS_DENIED.getMessage()));
    }

    @Test
    @DisplayName("게시물 작성자는 권한이 부족해도 게시물 조회 성공")
    void getOne_success_postWriterBypassesBoardAccess() throws Exception {
        // given
        signup(WRITER_LOGIN_ID);
        Long writerId = getUserId(WRITER_LOGIN_ID);

        Board board = boardRepository.save(Board.register(BOARD_NAME, UserRole.SILVER));
        Post post = postRepository.save(Post.register(writerId, board.getId(), POST_TITLE, POST_CONTENT, PostType.GENERAL));

        String accessToken = getAccessToken(WRITER_LOGIN_ID);

        // when & then
        mockMvc.perform(get("/api/posts/{postId}", post.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }


    // ========== 실제 QueryDSL 조회 로직 ==========
    @Test
    @DisplayName("고정글은 첫 페이지 상단에 표시")
    void getAll_success_pinnedPostOnTopOfTheFirstPage() throws Exception {
        // given
        signup(WRITER_LOGIN_ID);
        Long writerId = getUserId(WRITER_LOGIN_ID);

        Board freeBoard = boardRepository.save(Board.register(BOARD_NAME, null));
        Board noticeBoard = boardRepository.save(Board.register("공지게시판", null));

        Post generalPost = postRepository.save(Post.register(writerId, freeBoard.getId(), "일반글", POST_CONTENT, PostType.GENERAL));
        Post pinnedPost = postRepository.save(Post.register(writerId, noticeBoard.getId(), "고정글", POST_CONTENT, PostType.NOTICE));
        pinnedPost.pin();

        // when & then
        mockMvc.perform(get("/api/posts")
                        .param("boardId", freeBoard.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].id").value(pinnedPost.getId()))
                .andExpect(jsonPath("$.data.content[1].id").value(generalPost.getId()));
    }

    @Test
    @DisplayName("삭제된 게시물은 목록에서 제외")
    void getAll_success_deletedPostExcluded() throws Exception {
        // given
        signup(WRITER_LOGIN_ID);
        Long writerId = getUserId(WRITER_LOGIN_ID);

        Board board = boardRepository.save(Board.register(BOARD_NAME, null));
        Post post = postRepository.save(Post.register(writerId, board.getId(), POST_TITLE, POST_CONTENT, PostType.GENERAL));
        Post deletedPost = postRepository.save(Post.register(writerId, board.getId(), "삭제된 게시물", POST_CONTENT, PostType.GENERAL));
        deletedPost.delete();

        // when & then
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(post.getId()));
    }

    @Test
    @DisplayName("삭제된 댓글은 댓글 수 집계에서 제외")
    void getAll_success_deletedCommentExcludedFromTheCount() throws Exception {
        // given
        signup(WRITER_LOGIN_ID);
        Long writerId = getUserId(WRITER_LOGIN_ID);

        Board board = boardRepository.save(Board.register(BOARD_NAME, null));
        Post post = postRepository.save(Post.register(writerId, board.getId(), POST_TITLE, POST_CONTENT, PostType.GENERAL));

        commentRepository.save(Comment.register(null, post.getId(), writerId, COMMENT_CONTENT, 0));
        Comment deletedComment = commentRepository.save(Comment.register(null, post.getId(), writerId, "삭제된 댓글", 0));
        deletedComment.delete();

        // when & then
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].commentCount").value(1));
    }

    @Test
    @DisplayName("작성자 닉네임으로 게시물 검색 성공")
    void getAll_success_searchByWriterId() throws Exception {
        // given
        signup(WRITER_LOGIN_ID);
        Long writerId = getUserId(WRITER_LOGIN_ID);

        signup(USER_LOGIN_ID);
        Long userId = getUserId(USER_LOGIN_ID);

        Board board = boardRepository.save(Board.register(BOARD_NAME, null));
        Post writerPost = postRepository.save(Post.register(writerId, board.getId(), POST_TITLE, POST_CONTENT, PostType.GENERAL));
        postRepository.save(Post.register(userId, board.getId(), POST_TITLE, POST_CONTENT, PostType.GENERAL));

        // when & then
        mockMvc.perform(get("/api/posts")
                        .param("keyword", "작성자")
                        .param("searchType", "AUTHOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(writerPost.getId()));
    }


    // ========== 헬퍼 메서드 ==========
    private void signup(String loginId) throws Exception {
        String nickname = switch (loginId) {
            case MANAGER_LOGIN_ID -> "매니저";
            case WRITER_LOGIN_ID -> "작성자";
            case USER_LOGIN_ID -> "일반사용자";
            default -> "";
        };

        UserSignupRequest request = new UserSignupRequest(loginId, nickname, PASSWORD);

        mockMvc.perform(post("/api/auth/signup")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        User user = userRepository.findByLoginIdAndDeletedAtIsNull(loginId).orElseThrow();
        if (user.getLoginId().equals(MANAGER_LOGIN_ID)) {
            user.updateRoleByManager(UserRole.MANAGER);
        }
        userRepository.save(user);
    }

    private String getAccessToken(String loginId) throws Exception {
        LoginRequest loginRequest = new LoginRequest(loginId, PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("data").get("accessToken").asString();
    }

    private Long getUserId(String loginId) {
        return userRepository.findByLoginIdAndDeletedAtIsNull(loginId).orElseThrow().getId();
    }
}
