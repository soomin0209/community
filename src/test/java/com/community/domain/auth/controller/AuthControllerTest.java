package com.community.domain.auth.controller;

import com.community.common.config.security.CustomUserDetails;
import com.community.common.config.web.SuspendedCheckInterceptor;
import com.community.common.config.web.UserVisitInterceptor;
import com.community.common.config.web.WebMvcConfig;
import com.community.common.exception.GlobalExceptionHandler;
import com.community.domain.auth.dto.request.LoginRequest;
import com.community.domain.auth.dto.request.UserSignupRequest;
import com.community.domain.auth.dto.response.LoginResponse;
import com.community.domain.auth.dto.response.SignupResponse;
import com.community.domain.auth.service.AuthService;
import com.community.domain.user.enums.UserRole;
import jakarta.servlet.http.Cookie;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {WebMvcConfig.class, SuspendedCheckInterceptor.class, UserVisitInterceptor.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "cookie.secure=false")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;


    // ========== 회원가입 ==========
    @Test
    @DisplayName("회원가입 성공")
    void signup_success() throws Exception {
        // given
        UserSignupRequest request = new UserSignupRequest("bronze123", "브론즈유저", "password123@");
        SignupResponse response = new SignupResponse(1L, "bronze123", "브론즈유저");

        given(authService.signup(any(UserSignupRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loginId").value("bronze123"))
                .andExpect(jsonPath("$.data.nickname").value("브론즈유저"));
    }

    @Test
    @DisplayName("회원가입 실패 - 아이디 공백")
    void signup_fail_LoginIdIsNull() throws Exception {
        // given
        UserSignupRequest request = new UserSignupRequest(null, "브론즈유저", "password123@");

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("아이디를 입력해주세요"));
    }

    @Test
    @DisplayName("회원가입 실패 - 아이디 형식 불일치")
    void signup_fail_invalidLoginId() throws Exception {
        // given
        UserSignupRequest request = new UserSignupRequest("bronze", "브론즈유저", "password123@");

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("아이디 형식이 올바르지 않습니다"));
    }

    @Test
    @DisplayName("회원가입 실패 - 닉네임 공백")
    void signup_fail_nicknameIsNull() throws Exception {
        // given
        UserSignupRequest request = new UserSignupRequest("bronze123", null, "password123@");

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("닉네임을 입력해주세요"));
    }

    @Test
    @DisplayName("회원가입 실패 - 닉네임 형식 불일치")
    void signup_fail_invalidNickname() throws Exception {
        // given
        UserSignupRequest request = new UserSignupRequest("bronze123", "b", "password123@");

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("닉네임은 2~16자여야 합니다"));
    }

    @Test
    @DisplayName("회원가입 실패 - 비밀번호 공백")
    void signup_fail_passwordIsNull() throws Exception {
        // given
        UserSignupRequest request = new UserSignupRequest("bronze123", "브론즈유저", null);

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("비밀번호를 입력해주세요"));
    }

    @Test
    @DisplayName("회원가입 실패 - 비밀번호 형식 불일치")
    void signup_fail_invalidPassword() throws Exception {
        // given
        UserSignupRequest request = new UserSignupRequest("bronze123", "브론즈유저", "password");

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("비밀번호 형식이 올바르지 않습니다"));
    }


    // ========== 로그인 ==========
    @Test
    @DisplayName("로그인 성공")
    void login_success() throws Exception {
        // given
        LoginRequest request = new LoginRequest("bronze123", "password123@");
        LoginResponse response = new LoginResponse("accessToken", null);

        given(authService.login(any(LoginRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("accessToken"));
    }

    @Test
    @DisplayName("로그인 실패 - 아이디 공백")
    void login_fail_loginIdIsNull() throws Exception {
        // given
        LoginRequest request = new LoginRequest(null, "password123@");

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("아이디를 입력해주세요"));
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 공백")
    void login_fail_passwordIsNull() throws Exception {
        // given
        LoginRequest request = new LoginRequest("bronze123", null);

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("비밀번호를 입력해주세요"));
    }


    // ========== 토큰 재발급 ==========
    @Test
    @DisplayName("토큰 재발급 성공")
    void reissue_success() throws Exception {
        // given
        LoginResponse response = new LoginResponse("accessToken", null);

        Cookie cookie = new Cookie("refreshToken", "refreshToken");

        given(authService.reissue("refreshToken")).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .contentType(objectMapper.writeValueAsString(response))
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("accessToken"));
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 리프레시 토큰 없음")
    void reissue_fail_refreshTokenNotFound() throws Exception {
        // when & then
        mockMvc.perform(post("/api/auth/reissue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("리프레시 토큰이 없습니다"));
    }


    // ========== 로그아웃 ==========
    @Test
    @DisplayName("로그아웃 성공")
    void logout_success() throws Exception {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(1L, UserRole.BRONZE.name());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        willDoNothing().given(authService).logout(1L, "accessToken");

        // when & then
        mockMvc.perform(post("/api/auth/logout")
                        .requestAttr("accessToken", "accessToken"))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("refreshToken", 0))
                .andExpect(jsonPath("$.success").value(true));

        verify(authService).logout(1L, "accessToken");
    }
}