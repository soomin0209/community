package com.community.domain.board;

import com.community.domain.auth.dto.request.AdminSignupRequest;
import com.community.domain.auth.dto.request.LoginRequest;
import com.community.domain.auth.dto.request.UserSignupRequest;
import com.community.domain.board.dto.request.CreateBoardRequest;
import com.community.domain.board.entity.Board;
import com.community.domain.board.exception.BoardExceptionEnum;
import com.community.domain.board.repository.BoardRepository;
import com.community.domain.post.entity.Post;
import com.community.domain.post.enums.PostType;
import com.community.domain.post.repository.PostRepository;
import com.community.domain.user.entity.User;
import com.community.domain.user.enums.UserRole;
import com.community.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BoardIntegrationTest {

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

    @Value("${admin.key}")
    private String adminKey;

    private static final String ADMIN_LOGIN_ID = "admin123";
    private static final String MANAGER_LOGIN_ID = "manager123";
    private static final String PASSWORD = "password123@";
    private static final String BOARD_NAME = "자유게시판";


    // ========== 실제 Security 필터 체인 + ROLE_ADMIN 접근 제어 ==========
    @Test
    @DisplayName("관리자 권한으로 게시판 생성 성공")
    void create_success_byAdmin() throws Exception {
        // given
        String accessToken = getAdminAccessToken();
        CreateBoardRequest request = new CreateBoardRequest(BOARD_NAME, null);

        // when & then
        mockMvc.perform(post("/api/admin/boards")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value(BOARD_NAME));
    }

    @Test
    @DisplayName("매니저 권한으로 게시판 생성 시도 - 403")
    void create_fail_byManager_forbidden() throws Exception {
        // given
        String accessToken = getManagerAccessToken();
        CreateBoardRequest request = new CreateBoardRequest(BOARD_NAME, null);

        // when & then
        mockMvc.perform(post("/api/admin/boards")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증 없이 게시판 생성 시도 - 401")
    void create_fail_noToken_unauthorized() throws Exception {
        // given
        CreateBoardRequest request = new CreateBoardRequest(BOARD_NAME, null);

        // when & then
        mockMvc.perform(post("/api/admin/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }


    // ========== 실제 Post 테이블과의 연동 (게시판 삭제 제약) ==========
    @Test
    @DisplayName("게시물이 존재하는 게시판 삭제 시도 - 409")
    void delete_fail_boardInUse() throws Exception {
        // given
        String accessToken = getAdminAccessToken();

        Board board = boardRepository.save(Board.register(BOARD_NAME, null));
        postRepository.save(Post.register(1L, board.getId(), "제목", "내용", PostType.GENERAL));

        // when & then
        mockMvc.perform(delete("/api/admin/boards/{boardId}", board.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(BoardExceptionEnum.BOARD_IN_USE.getMessage()));
    }

    @Test
    @DisplayName("게시물이 삭제된 게시판은 삭제 성공")
    void delete_success_whenPostIsDeleted() throws Exception {
        // given
        String accessToken = getAdminAccessToken();

        Board board = boardRepository.save(Board.register(BOARD_NAME, null));
        Post post = postRepository.save(Post.register(1L, board.getId(), "제목", "내용", PostType.GENERAL));
        post.delete();

        // when & then
        mockMvc.perform(delete("/api/admin/boards/{boardId}", board.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }


    // ========== 헬퍼 메서드 ==========
    private String getAdminAccessToken() throws Exception {
        AdminSignupRequest request = new AdminSignupRequest(ADMIN_LOGIN_ID, "관리자", PASSWORD, adminKey);

        mockMvc.perform(post("/api/admin/auth/signup")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        return getAccessToken(ADMIN_LOGIN_ID);
    }

    private String getManagerAccessToken() throws Exception {
        UserSignupRequest request = new UserSignupRequest(MANAGER_LOGIN_ID, "매니저", PASSWORD);

        mockMvc.perform(post("/api/auth/signup")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        User manager = userRepository.findByLoginIdAndDeletedAtIsNull(MANAGER_LOGIN_ID).orElseThrow();
        manager.updateRoleByManager(UserRole.MANAGER);
        userRepository.save(manager);

        return getAccessToken(MANAGER_LOGIN_ID);
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
}
