package com.community.domain.auth.controller;

import com.community.common.config.web.SuspendedCheckInterceptor;
import com.community.common.config.web.UserVisitInterceptor;
import com.community.common.config.web.WebMvcConfig;
import com.community.common.exception.GlobalExceptionHandler;
import com.community.domain.auth.dto.request.AdminSignupRequest;
import com.community.domain.auth.dto.response.SignupResponse;
import com.community.domain.auth.service.AuthAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {AuthAdminController.class, GlobalExceptionHandler.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {WebMvcConfig.class, SuspendedCheckInterceptor.class, UserVisitInterceptor.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
class AuthAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthAdminService authAdminService;


    // ========== 관리자 회원가입 ==========
    @Test
    @DisplayName("관리자 회원가입 성공")
    void signup_success() throws Exception {
        // given
        AdminSignupRequest request = new AdminSignupRequest("admin123", "관리자", "password123@", "adminsecretkey");
        SignupResponse response = new SignupResponse(1L, "admin123", "관리자");

        given(authAdminService.signup(any(AdminSignupRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/admin/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loginId").value("admin123"))
                .andExpect(jsonPath("$.data.nickname").value("관리자"));
    }

    @Test
    @DisplayName("관리자 회원가입 실패 - 아이디 공백")
    void signup_fail_LoginIdIsNull() throws Exception {
        // given
        AdminSignupRequest request = new AdminSignupRequest(null, "관리자", "password123@", "adminsecretkey");

        // when & then
        mockMvc.perform(post("/api/admin/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("아이디를 입력해주세요"));
    }

    @Test
    @DisplayName("관리자 회원가입 실패 - 아이디 형식 불일치")
    void signup_fail_invalidLoginId() throws Exception {
        // given
        AdminSignupRequest request = new AdminSignupRequest("admin", "관리자", "password123@", "adminsecretkey");

        // when & then
        mockMvc.perform(post("/api/admin/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("아이디 형식이 올바르지 않습니다"));
    }

    @Test
    @DisplayName("관리자 회원가입 실패 - 닉네임 공백")
    void signup_fail_nicknameIsNull() throws Exception {
        // given
        AdminSignupRequest request = new AdminSignupRequest("admin123", null, "password123@", "adminsecretkey");

        // when & then
        mockMvc.perform(post("/api/admin/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("닉네임을 입력해주세요"));
    }

    @Test
    @DisplayName("관리자 회원가입 실패 - 닉네임 형식 불일치")
    void signup_fail_invalidNickname() throws Exception {
        // given
        AdminSignupRequest request = new AdminSignupRequest("admin123", "a", "password123@", "adminsecretkey");

        // when & then
        mockMvc.perform(post("/api/admin/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("닉네임은 2~16자여야 합니다"));
    }

    @Test
    @DisplayName("관리자 회원가입 실패 - 비밀번호 공백")
    void signup_fail_passwordIsNull() throws Exception {
        // given
        AdminSignupRequest request = new AdminSignupRequest("admin123", "관리자", null, "adminsecretkey");

        // when & then
        mockMvc.perform(post("/api/admin/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("비밀번호를 입력해주세요"));
    }

    @Test
    @DisplayName("관리자 회원가입 실패 - 비밀번호 형식 불일치")
    void signup_fail_invalidPassword() throws Exception {
        // given
        AdminSignupRequest request = new AdminSignupRequest("admin123", "관리자", "password", "adminsecretkey");

        // when & then
        mockMvc.perform(post("/api/admin/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("비밀번호 형식이 올바르지 않습니다"));
    }

    @Test
    @DisplayName("관리자 회원가입 실패 - 관리자 인증키 공백")
    void signup_fail_adminKeyIsNull() throws Exception {
        // given
        AdminSignupRequest request = new AdminSignupRequest("admin123", "관리자", "password123@", null);

        // when & then
        mockMvc.perform(post("/api/admin/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("관리자 인증키를 입력해주세요"));
    }
}