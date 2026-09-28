package com.community.domain.reaction;

import com.community.domain.auth.dto.request.LoginRequest;
import com.community.domain.auth.dto.request.UserSignupRequest;
import com.community.domain.board.entity.Board;
import com.community.domain.board.exception.BoardExceptionEnum;
import com.community.domain.board.repository.BoardRepository;
import com.community.domain.post.entity.Post;
import com.community.domain.post.enums.PostType;
import com.community.domain.post.repository.PostRepository;
import com.community.domain.reaction.dto.request.ReactionRequest;
import com.community.domain.reaction.enums.ReactionType;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReactionIntegrationTest {

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

    private static final String USER_LOGIN_ID = "user123";
    private static final String WRITER_LOGIN_ID = "writer123";
    private static final String PASSWORD = "password123@";
    private static final String BOARD_NAME = "자유게시판";
    private static final String POST_TITLE = "게시물 제목";
    private static final String POST_CONTENT = "게시물 내용";

    @AfterEach
    void cleanUp() {
        List.of(USER_LOGIN_ID, WRITER_LOGIN_ID).forEach(loginId ->
                userRepository.findByLoginIdAndDeletedAtIsNull(loginId)
                        .ifPresent(u -> userRepository.delete(u)));
    }


    // ========== 실제 Security 필터 체인 ==========
    @Test
    @DisplayName("인증 없이 좋아요 시도 - 401")
    void react_fail_noToken_unauthorized() throws Exception {
        // given
        signup(WRITER_LOGIN_ID);
        Long writerId = getUserId(WRITER_LOGIN_ID);

        Board board = boardRepository.save(Board.register(BOARD_NAME, null));
        Post post = postRepository.save(Post.register(writerId, board.getId(), POST_TITLE, POST_CONTENT, PostType.GENERAL));

        ReactionRequest request = new ReactionRequest(ReactionType.LIKE);

        // when & then
        mockMvc.perform(post("/api/posts/{postId}/reactions", post.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }


    // ========== 실제 게시판 권한 검증 ==========
    @Test
    @DisplayName("접근 권한이 없는 게시판 게시물에 좋아요 시도 - 403")
    void react_fail_boardAccessDenied() throws Exception {
        // given
        signup(WRITER_LOGIN_ID);
        Long writerId = getUserId(WRITER_LOGIN_ID);

        Board board = boardRepository.save(Board.register(BOARD_NAME, UserRole.SILVER));
        Post post = postRepository.save(Post.register(writerId, board.getId(), POST_TITLE, POST_CONTENT, PostType.GENERAL));

        signup(USER_LOGIN_ID);
        String accessToken = getAccessToken(USER_LOGIN_ID);

        ReactionRequest request = new ReactionRequest(ReactionType.LIKE);

        // when & then
        mockMvc.perform(post("/api/posts/{postId}/reactions", post.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(BoardExceptionEnum.BOARD_ACCESS_DENIED.getMessage()));
    }


    // ========== 실제 DB 반영 ==========
    @Test
    @DisplayName("좋아요 -> 싫어요 전환 시 변경 감지로 DB 반영")
    void react_success_switchLikeToDislike() throws Exception {
        // given
        signup(WRITER_LOGIN_ID);
        Long writerId = getUserId(WRITER_LOGIN_ID);

        Board board = boardRepository.save(Board.register(BOARD_NAME, null));
        Post post = postRepository.save(Post.register(writerId, board.getId(), POST_TITLE, POST_CONTENT, PostType.GENERAL));

        signup(USER_LOGIN_ID);
        String accessToken = getAccessToken(USER_LOGIN_ID);

        ReactionRequest likeRequest = new ReactionRequest(ReactionType.LIKE);
        ReactionRequest dislikeRequest = new ReactionRequest(ReactionType.DISLIKE);

        mockMvc.perform(post("/api/posts/{postId}/reactions", post.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(likeRequest)))
                .andExpect(status().isOk());

        // when
        mockMvc.perform(post("/api/posts/{postId}/reactions", post.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dislikeRequest)))
                .andExpect(status().isOk());

        // then
        mockMvc.perform(get("/api/posts/{postId}", post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(0))
                .andExpect(jsonPath("$.data.dislikeCount").value(1));
    }


    // ========== 헬퍼 메서드 ==========
    private void signup(String loginId) throws Exception {
        String nickname = switch (loginId) {
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
