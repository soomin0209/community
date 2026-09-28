package com.community.domain.user;

import com.community.domain.auth.dto.request.LoginRequest;
import com.community.domain.auth.dto.request.UserSignupRequest;
import com.community.domain.user.dto.request.SuspendUserRequest;
import com.community.domain.user.dto.request.UpdateUserNicknameRequest;
import com.community.domain.user.dto.request.UpdateUserRoleRequest;
import com.community.domain.user.entity.User;
import com.community.domain.user.enums.UserRole;
import com.community.domain.user.exception.UserExceptionEnum;
import com.community.domain.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.transaction.Transactional;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private static final String MANAGER_LOGIN_ID = "manager123";
    private static final String USER_LOGIN_ID = "user123";
    private static final String PASSWORD = "password123@";
    private static final String SUSPENDED_REASON = "욕설 사용";


    // ========== 실제 Security 필터 체인 (GET 공개 / 마이페이지 인증) ==========
    @Test
    @DisplayName("인증 없이 마이페이지 조회 시도 - 401")
    void getMine_fail_noToken_unauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }


    // ========== 실제 Security 필터 체인 + ROLE_MANAGER 접근 제어 ==========
    @Test
    @DisplayName("매니저 권한으로 회원 정지 성공")
    void suspend_success_byManager() throws Exception {
        // given
        signup(USER_LOGIN_ID);
        Long userId = getUserId(USER_LOGIN_ID);

        signup(MANAGER_LOGIN_ID);
        String accessToken = getAccessToken(MANAGER_LOGIN_ID);

        SuspendUserRequest request = new SuspendUserRequest(SUSPENDED_REASON, 7, false);

        // when & then
        mockMvc.perform(patch("/api/manager/users/{userId}/suspend", userId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("일반 사용자가 회원 정지 시도 - 403")
    void suspend_fail_byGeneralUser_forbidden() throws Exception {
        // given
        signup(MANAGER_LOGIN_ID);
        Long managerId = getUserId(MANAGER_LOGIN_ID);

        signup(USER_LOGIN_ID);
        String accessToken = getAccessToken(USER_LOGIN_ID);

        SuspendUserRequest request = new SuspendUserRequest(SUSPENDED_REASON, 7, false);

        // when & then
        mockMvc.perform(patch("/api/manager/users/{userId}/suspend", managerId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증 없이 회원 정지 시도 - 401")
    void suspend_fail_noToken_unauthorized() throws Exception {
        // given
        signup(USER_LOGIN_ID);
        Long userId = getUserId(USER_LOGIN_ID);

        SuspendUserRequest request = new SuspendUserRequest(SUSPENDED_REASON, 7, false);

        // when & then
        mockMvc.perform(patch("/api/manager/users/{userId}/suspend", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }


    // ========== 실제 SuspendedCheckInterceptor ==========
    @Test
    @DisplayName("정지된 사용자가 닉네임 변경 시도 - 403")
    void updateNickname_fail_suspendedUser() throws Exception {
        // given
        signup(USER_LOGIN_ID);
        String accessToken = getAccessToken(USER_LOGIN_ID);

        User user = userRepository.findByLoginIdAndDeletedAtIsNull(USER_LOGIN_ID).orElseThrow();
        user.updateSuspendedUntil(LocalDateTime.now(), 7, false);

        UpdateUserNicknameRequest request = new UpdateUserNicknameRequest("새 닉네임");

        // when & then
        mockMvc.perform(patch("/api/users/nickname")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(UserExceptionEnum.USER_SUSPENDED.getMessage()));
    }


    // ========== 실제 Redis 블랙리스트 + JWT 필터 (토큰 무효화) ==========
    @Test
    @DisplayName("회원 탈퇴 후 기존 토큰으로 요청 - 401")
    void withdraw_then_reuseToken_fail() throws Exception {
        // given
        signup(USER_LOGIN_ID);
        String accessToken = getAccessToken(USER_LOGIN_ID);

        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // when & then
        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("등급 변경 전 발급된 토큰으로 요청 - 401")
    void updateRole_then_reuseToken_fail() throws Exception {
        // given
        signup(USER_LOGIN_ID);
        Long userId = getUserId(USER_LOGIN_ID);
        String oldAccessToken = getAccessToken(USER_LOGIN_ID);

        signup(MANAGER_LOGIN_ID);
        String managerAccessToken = getAccessToken(MANAGER_LOGIN_ID);

        Thread.sleep(1000); // 1초 대기

        UpdateUserRoleRequest request = new UpdateUserRoleRequest(UserRole.SILVER);

        mockMvc.perform(patch("/api/manager/users/{userId}/role", userId)
                        .header("Authorization", "Bearer " + managerAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // when & then
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + oldAccessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("등급 변경 후 재발급한 토큰으로 요청 성공")
    void updateRole_then_reissue_success() throws Exception {
        // given
        signup(USER_LOGIN_ID);
        Long userId = getUserId(USER_LOGIN_ID);
        Cookie refreshTokenCookie = getRefreshTokenCookie(USER_LOGIN_ID);

        signup(MANAGER_LOGIN_ID);
        String managerAccessToken = getAccessToken(MANAGER_LOGIN_ID);

        UpdateUserRoleRequest request = new UpdateUserRoleRequest(UserRole.SILVER);

        mockMvc.perform(patch("/api/manager/users/{userId}/role", userId)
                        .header("Authorization", "Bearer " + managerAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(post("/api/auth/reissue")
                        .cookie(refreshTokenCookie))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        String newAccessToken = node.get("data").get("accessToken").asString();

        // when & then
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value(UserRole.SILVER.name()));
    }


    // ========== 헬퍼 메서드 ==========
    private void signup(String loginId) throws Exception {
        String nickname = switch (loginId) {
            case MANAGER_LOGIN_ID -> "매니저";
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

    private MvcResult login(String loginId) throws Exception {
        LoginRequest request = new LoginRequest(loginId, PASSWORD);

        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String getAccessToken(String loginId) throws Exception {
        MvcResult result = login(loginId);
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("data").get("accessToken").asString();
    }

    private Cookie getRefreshTokenCookie(String loginId) throws Exception {
        MvcResult result = login(loginId);
        return result.getResponse().getCookie("refreshToken");
    }

    private Long getUserId(String loginId) {
        return userRepository.findByLoginIdAndDeletedAtIsNull(loginId).orElseThrow().getId();
    }
}
