package com.community.domain.comment;

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
class CommentIntegrationTest {

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


    // ========== 실제 Security 필터 체인 + ROLE_MANAGER 접근 제어 ==========
    @Test
    @DisplayName("매니저 권한으로 댓글 강제 삭제 성공")
    void force_delete_success_byManager() throws Exception {
        // given
        signup(WRITER_LOGIN_ID);
        Long writerId = getUserId(WRITER_LOGIN_ID);

        Board board = boardRepository.save(Board.register(BOARD_NAME, null));
        Post post = postRepository.save(Post.register(writerId, board.getId(), POST_TITLE, POST_CONTENT, PostType.GENERAL));
        Comment comment = commentRepository.save(Comment.register(null, post.getId(), writerId, COMMENT_CONTENT, 0));

        signup(MANAGER_LOGIN_ID);
        String accessToken = getAccessToken(MANAGER_LOGIN_ID);

        // when & then
        mockMvc.perform(delete("/api/manager/comments/{commentId}", comment.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("일반 사용자가 댓글 강제 삭제 시도 - 403")
    void force_delete_fail_byGeneralUser_forbidden() throws Exception {
        // given
        signup(WRITER_LOGIN_ID);
        Long writerId = getUserId(WRITER_LOGIN_ID);

        Board board = boardRepository.save(Board.register(BOARD_NAME, null));
        Post post = postRepository.save(Post.register(writerId, board.getId(), POST_TITLE, POST_CONTENT, PostType.GENERAL));
        Comment comment = commentRepository.save(Comment.register(null, post.getId(), writerId, COMMENT_CONTENT, 0));

        signup(USER_LOGIN_ID);
        String accessToken = getAccessToken(USER_LOGIN_ID);

        // when & then
        mockMvc.perform(delete("/api/manager/comments/{commentId}", comment.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증 없이 강제 삭제 시도 - 401")
    void force_delete_fail_noToken_unauthorized() throws Exception {
        // given
        signup(WRITER_LOGIN_ID);
        Long writerId = getUserId(WRITER_LOGIN_ID);

        Board board = boardRepository.save(Board.register(BOARD_NAME, null));
        Post post = postRepository.save(Post.register(writerId, board.getId(), POST_TITLE, POST_CONTENT, PostType.GENERAL));
        Comment comment = commentRepository.save(Comment.register(null, post.getId(), writerId, COMMENT_CONTENT, 0));

        // when & then
        mockMvc.perform(delete("/api/manager/comments/{commentId}", comment.getId()))
                .andExpect(status().isUnauthorized());
    }


    // ========== 실제 게시판 권한 검증 + 작성자 우회 ==========
    @Test
    @DisplayName("접근 권한이 없는 사용자의 댓글 목록 조회 시도 - 403")
    void getAll_fail_boardAccessDenied() throws Exception {
        // given
        signup(WRITER_LOGIN_ID);
        Long writerId = getUserId(WRITER_LOGIN_ID);

        Board board = boardRepository.save(Board.register(BOARD_NAME, UserRole.SILVER));
        Post post = postRepository.save(Post.register(writerId, board.getId(), POST_TITLE, POST_CONTENT, PostType.GENERAL));

        signup(USER_LOGIN_ID);
        String accessToken = getAccessToken(USER_LOGIN_ID);

        // when & then
        mockMvc.perform(get("/api/posts/{postId}/comments", post.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(BoardExceptionEnum.BOARD_ACCESS_DENIED.getMessage()));
    }

    @Test
    @DisplayName("게시물 작성자는 권한이 부족해도 댓글 목록 조회 성공")
    void getAll_success_postWriterBypassesBoardAccess() throws Exception {
        // given
        signup(WRITER_LOGIN_ID);
        Long writerId = getUserId(WRITER_LOGIN_ID);

        Board board = boardRepository.save(Board.register(BOARD_NAME, UserRole.SILVER));
        Post post = postRepository.save(Post.register(writerId, board.getId(), POST_TITLE, POST_CONTENT, PostType.GENERAL));

        String accessToken = getAccessToken(WRITER_LOGIN_ID);

        // when & then
        mockMvc.perform(get("/api/posts/{postId}/comments", post.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }


    // ========== 실제 QueryDSL 조회 로직 ==========
    @Test
    @DisplayName("부모-자식 댓글이 실제로 계층 구조로 조립되어 반환")
    void getAll_success_parentChildTree() throws Exception {
        // given
        signup(WRITER_LOGIN_ID);
        Long writerId = getUserId(WRITER_LOGIN_ID);

        signup(USER_LOGIN_ID);
        Long userId = getUserId(USER_LOGIN_ID);

        Board board = boardRepository.save(Board.register(BOARD_NAME, null));
        Post post = postRepository.save(Post.register(writerId, board.getId(), POST_TITLE, POST_CONTENT, PostType.GENERAL));

        Comment parent = commentRepository.save(Comment.register(null, post.getId(), writerId, "부모 댓글", 0));
        commentRepository.save(Comment.register(parent.getId(), post.getId(), userId, "자식 댓글", 1));

        // when & then
        mockMvc.perform(get("/api/posts/{postId}/comments", post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].content").value("부모 댓글"))
                .andExpect(jsonPath("$.data.content[0].children[0].content").value("자식 댓글"));
    }

    @Test
    @DisplayName("삭제된 댓글은 닉네임/내용이 마스킹되어 표시")
    void getAll_success_deletedCommentMasked() throws Exception {
        // given
        signup(WRITER_LOGIN_ID);
        Long writerId = getUserId(WRITER_LOGIN_ID);

        Board board = boardRepository.save(Board.register(BOARD_NAME, null));
        Post post = postRepository.save(Post.register(writerId, board.getId(), POST_TITLE, POST_CONTENT, PostType.GENERAL));
        Comment comment = commentRepository.save(Comment.register(null, post.getId(), writerId, COMMENT_CONTENT, 0));
        comment.delete();

        // when & then
        mockMvc.perform(get("/api/posts/{postId}/comments", post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].nickname").value("알 수 없음"))
                .andExpect(jsonPath("$.data.content[0].content").value("삭제된 댓글입니다"));
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
