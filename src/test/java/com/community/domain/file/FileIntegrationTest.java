package com.community.domain.file;

import com.community.domain.auth.dto.request.LoginRequest;
import com.community.domain.auth.dto.request.UserSignupRequest;
import com.community.domain.board.entity.Board;
import com.community.domain.board.exception.BoardExceptionEnum;
import com.community.domain.board.repository.BoardRepository;
import com.community.domain.file.entity.File;
import com.community.domain.file.repository.FileRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FileIntegrationTest {

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
    private FileRepository fileRepository;

    private static final String MANAGER_LOGIN_ID = "manager123";
    private static final String USER_LOGIN_ID = "user123";
    private static final String WRITER_LOGIN_ID = "writer123";
    private static final String PASSWORD = "password123@";
    private static final String BOARD_NAME = "자유게시판";
    private static final String POST_TITLE = "게시물 제목";
    private static final String POST_CONTENT = "게시물 내용";
    private static final String FILE_NAME = "테스트 파일.txt";
    private static final String FILE_STORED_PATH = "uploads/test/uuid";
    private static final byte[] FILE_CONTENT = "파일 내용".getBytes(StandardCharsets.UTF_8);

    private final List<Path> uploadedPaths = new ArrayList<>();

    @AfterEach
    void cleanUp() throws Exception {
        for (Path path : uploadedPaths) {
            Files.deleteIfExists(path);
        }

        List.of(MANAGER_LOGIN_ID, USER_LOGIN_ID, WRITER_LOGIN_ID).forEach(loginId ->
                userRepository.findByLoginIdAndDeletedAtIsNull(loginId)
                        .ifPresent(u -> userRepository.delete(u)));
    }


    // =========== 실제 Security 필터 체인 + ROLE_MANAGER 접근 제어 ==========
    @Test
    @DisplayName("매니저 권한으로 파일 강제 삭제 성공")
    void force_delete_success_byManager() throws Exception {
        // given
        signup(WRITER_LOGIN_ID);
        Long writerId = getUserId(WRITER_LOGIN_ID);

        File file = fileRepository.save(File.register(writerId, FILE_NAME, FILE_STORED_PATH, 100L, "text/plain"));

        signup(MANAGER_LOGIN_ID);
        String accessToken = getAccessToken(MANAGER_LOGIN_ID);

        // when & then
        mockMvc.perform(delete("/api/manager/files/{fileId}", file.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("일반 사용자가 파일 강제 삭제 시도 - 403")
    void force_delete_fail_byGeneralUser_forbidden() throws Exception {
        // given
        signup(WRITER_LOGIN_ID);
        Long writerId = getUserId(WRITER_LOGIN_ID);

        File file = fileRepository.save(File.register(writerId, FILE_NAME, FILE_STORED_PATH, 100L, "text/plain"));

        signup(USER_LOGIN_ID);
        String accessToken = getAccessToken(USER_LOGIN_ID);

        // when & then
        mockMvc.perform(delete("/api/manager/files/{fileId}", file.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증 없이 강제 삭제 시도 - 401")
    void force_delete_fail_noToken_unauthorized() throws Exception {
        // given
        signup(WRITER_LOGIN_ID);
        Long writerId = getUserId(WRITER_LOGIN_ID);

        File file = fileRepository.save(File.register(writerId, FILE_NAME, FILE_STORED_PATH, 100L, "text/plain"));

        // when & then
        mockMvc.perform(delete("/api/manager/files/{fileId}", file.getId()))
                .andExpect(status().isUnauthorized());
    }


    // ========== 실제 게시판 권한 검증 + 작성자 우회 ==========
    @Test
    @DisplayName("접근 권한이 없는 사용자의 첨부파일 다운로드 시도 - 403")
    void download_fail_boardAccessDenied() throws Exception {
        // given
        signup(WRITER_LOGIN_ID);
        Long writerId = getUserId(WRITER_LOGIN_ID);

        Board board = boardRepository.save(Board.register(BOARD_NAME, UserRole.SILVER));
        Post post = postRepository.save(Post.register(writerId, board.getId(), POST_TITLE, POST_CONTENT, PostType.GENERAL));
        File file = fileRepository.save(File.register(writerId, FILE_NAME, FILE_STORED_PATH, 100L, "text/plain"));
        file.attachToPost(post.getId());

        signup(USER_LOGIN_ID);
        String accessToken = getAccessToken(USER_LOGIN_ID);

        // when & then
        mockMvc.perform(get("/api/files/download/{fileId}", file.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(BoardExceptionEnum.BOARD_ACCESS_DENIED.getMessage()));
    }

    @Test
    @DisplayName("게시물 작성자는 권한이 부족해도 첨부파일 다운로드 성공")
    void download_success_postWriterBypassesBoardAccess() throws Exception {
        // given
        signup(WRITER_LOGIN_ID);
        Long writerId = getUserId(WRITER_LOGIN_ID);

        Board board = boardRepository.save(Board.register(BOARD_NAME, UserRole.SILVER));
        Post post = postRepository.save(Post.register(writerId, board.getId(), POST_TITLE, POST_CONTENT, PostType.GENERAL));

        String accessToken = getAccessToken(WRITER_LOGIN_ID);
        Long fileId = upload(accessToken, FILE_NAME);
        fileRepository.findById(fileId).orElseThrow().attachToPost(post.getId());

        // when & then
        mockMvc.perform(get("/api/files/download/{fileId}", fileId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }


    // ========== 실제 파일시스템 저장 -> 다운로드  ==========
    @Test
    @DisplayName("업로드한 파일을 다운로드하면 원본 내용 그대로 반환")
    void upload_then_download_success() throws Exception {
        // given
        signup(WRITER_LOGIN_ID);
        String accessToken = getAccessToken(WRITER_LOGIN_ID);
        Long fileId = upload(accessToken, FILE_NAME);

        // when & then
        mockMvc.perform(get("/api/files/download/{fileId}", fileId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(content().bytes(FILE_CONTENT));
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

    private Long upload(String accessToken, String filename) throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", filename, "text/plain", FILE_CONTENT);

        MvcResult result = mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get(0);
        uploadedPaths.add(Path.of(data.get("url").asString()));
        return data.get("id").asLong();
    }
}
