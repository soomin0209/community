package com.community.domain.auth;

import com.community.common.exception.CommonExceptionEnum;
import com.community.domain.auth.dto.request.LoginRequest;
import com.community.domain.auth.dto.request.UserSignupRequest;
import com.community.domain.auth.exception.AuthExceptionEnum;
import com.community.domain.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private static final String LOGIN_ID = "testlogin123";
    private static final String PASSWORD = "password123@";

    @AfterEach
    void cleanUp() {
        userRepository.findByLoginIdAndDeletedAtIsNull(LOGIN_ID)
                .ifPresent(u -> userRepository.delete(u));
    }


    // ========== Idempotency-Key 실동작 ==========
    @Test
    @DisplayName("회원가입 실패 - Idempotency-Key 헤더 누락")
    void signup_fail_missingIdempotencyKey() throws Exception {
        // given
        UserSignupRequest request = new UserSignupRequest(LOGIN_ID, "테스트", PASSWORD);

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(CommonExceptionEnum.MISSING_IDEMPOTENCY_KEY.getMessage()));
    }

    @Test
    @DisplayName("회원가입 실패 - 동일한 Idempotency-Key로 중복 요청")
    void signup_fail_duplicateIdempotencyKey() throws Exception {
        // given
        UserSignupRequest request = new UserSignupRequest(LOGIN_ID, "테스트", PASSWORD);
        String idempotencyKey = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/auth/signup")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(CommonExceptionEnum.DUPLICATE_REQUEST.getMessage()));
    }


    // ========== 회원가입 -> 로그인 ==========
    @Test
    @DisplayName("회원가입 후 로그인 성공 (Bcrypt 암호화 왕복)")
    void signup_then_login_success() throws Exception {
        // given
        signupTestUser();

        LoginRequest request = new LoginRequest(LOGIN_ID, PASSWORD);

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists());
    }


    // ========== 실제 JWT 필터 체인 + Redis 블랙리스트 ==========
    @Test
    @DisplayName("로그인 후 발급된 accessToken으로 인증 성공")
    void login_then_accessProtectedApi_success() throws Exception {
        // given
        signupTestUser();
        String accessToken = getAccessToken();

        // when & then
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Authorization 헤더 없이 API 접근 - 401")
    void accessProtectedApi_fail_noToken() throws Exception {
        // when & then
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그아웃된 토큰으로 재요청 - 블랙리스트 검증 401")
    void logout_then_reuseSameToken_fail() throws Exception {
        // given
        signupTestUser();
        String accessToken = getAccessToken();

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // when & then
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }


    // ========== Redis 리프레시 토큰으로 토큰 재발급 ==========
    @Test
    @DisplayName("토큰 재발급 후 새 accessToken으로 인증 성공")
    void reissue_success() throws Exception {
        // given
        signupTestUser();
        Cookie refreshTokenCookie = getRefreshTokenCookie();

        // when
        MvcResult result = mockMvc.perform(post("/api/auth/reissue")
                        .cookie(refreshTokenCookie))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        String newAccessToken = node.get("data").get("accessToken").asString();

        // then
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("무효화된 refreshToken 재사용으로 토큰 재발급 실패")
    void reissue_fail_reuseOldRefreshToken() throws Exception {
        // given
        signupTestUser();
        Cookie oldRefreshTokenCookie = getRefreshTokenCookie();

        // 재발급으로 기존 refreshToken 무효화
        mockMvc.perform(post("/api/auth/reissue")
                        .cookie(oldRefreshTokenCookie))
                .andExpect(status().isOk());

        // when & then
        // 무효화된 refreshToken 재사용
        mockMvc.perform(post("/api/auth/reissue")
                        .cookie(oldRefreshTokenCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(AuthExceptionEnum.INVALID_REFRESH_TOKEN.getMessage()));
    }


    // ========== 헬퍼 메서드 ==========
    // 회원가입
    private void signupTestUser() throws Exception {
        UserSignupRequest request = new UserSignupRequest(LOGIN_ID, "테스트", PASSWORD);

        mockMvc.perform(post("/api/auth/signup")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    // 로그인
    private MvcResult loginTestUser() throws Exception {
        LoginRequest request = new LoginRequest(LOGIN_ID, PASSWORD);

        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
    }

    // accessToken 추출
    private String getAccessToken() throws Exception {
        MvcResult result = loginTestUser();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("data").get("accessToken").asString();
    }

    // refreshToken 쿠키 추출
    private Cookie getRefreshTokenCookie() throws Exception {
        MvcResult result = loginTestUser();
        return result.getResponse().getCookie("refreshToken");
    }
}
