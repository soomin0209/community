package com.community.domain.user.controller;

import com.community.common.config.security.CustomUserDetails;
import com.community.common.config.web.SuspendedCheckInterceptor;
import com.community.common.config.web.UserVisitInterceptor;
import com.community.common.config.web.WebMvcConfig;
import com.community.domain.user.dto.request.UpdateUserNicknameRequest;
import com.community.domain.user.dto.request.UpdateUserPasswordRequest;
import com.community.domain.user.dto.response.*;
import com.community.domain.user.enums.UserRankType;
import com.community.domain.user.enums.UserRole;
import com.community.domain.user.service.UserRankingService;
import com.community.domain.user.service.UserService;
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
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {WebMvcConfig.class, SuspendedCheckInterceptor.class, UserVisitInterceptor.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRankingService userRankingService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }


    // ========== 프로필 조회 ==========
    @Test
    @DisplayName("프로필 조회 성공")
    void getOne_success() throws Exception {
        // given
        GetOneUserResponse response = new GetOneUserResponse(1L, "브론즈유저", LocalDateTime.now(),
                1L, 1L, 1L, UserRole.BRONZE);

        given(userService.getOne(eq(1L))).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/users/{userId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nickname").value("브론즈유저"))
                .andExpect(jsonPath("$.data.role").value(UserRole.BRONZE.name()));
    }


    // ========== 마이페이지 조회 ==========
    @Test
    @DisplayName("마이페이지 조회")
    void getMine_success() throws Exception {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(1L, UserRole.BRONZE.name());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        GetMypageResponse response = new GetMypageResponse(1L, "bronze123", "브론즈유저", LocalDateTime.now(),
                1L, 1L, 1L, UserRole.BRONZE, null, null);

        given(userService.getMine(eq(1L))).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.loginId").value("bronze123"))
                .andExpect(jsonPath("$.data.nickname").value("브론즈유저"))
                .andExpect(jsonPath("$.data.role").value(UserRole.BRONZE.name()));
    }


    // ========== 주간 사용자 랭킹 조회 ==========
    @Test
    @DisplayName("주간 사용자 랭킹 조회 성공")
    void getRanking_success() throws Exception {
        // given
        List<GetUserRankingResponse> response = List.of(
                new GetUserRankingResponse(1L, "브론즈유저", 50L),
                new GetUserRankingResponse(2L, "실버유저", 40L),
                new GetUserRankingResponse(3L, "골드유저", 30L),
                new GetUserRankingResponse(4L, "매니저", 20L),
                new GetUserRankingResponse(5L, "관리자", 10L)
        );

        given(userRankingService.getWeeklyUserRanking(eq(UserRankType.COMMENT))).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/users/ranking")
                        .param("type", UserRankType.COMMENT.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].nickname").value("브론즈유저"))
                .andExpect(jsonPath("$.data[0].count").value(50L));
    }


    // ========== 닉네임 변경 ==========
    @Test
    @DisplayName("닉네임 변경 성공")
    void updateNickname_success() throws Exception {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(1L, UserRole.BRONZE.name());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        UpdateUserNicknameRequest request = new UpdateUserNicknameRequest("브론즈");
        UpdateUserNicknameResponse response = new UpdateUserNicknameResponse(1L, "브론즈", LocalDateTime.now(), LocalDateTime.now());

        given(userService.updateNickname(eq(1L), any(UpdateUserNicknameRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/users/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nickname").value("브론즈"));
    }

    @Test
    @DisplayName("닉네임 변경 실패 - 변경할 닉네임 공백")
    void updateNickname_fail_nicknameIsNull() throws Exception {
        // given
        UpdateUserNicknameRequest request = new UpdateUserNicknameRequest(null);

        // when & then
        mockMvc.perform(patch("/api/users/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("변경할 닉네임을 입력해주세요"));
    }

    @Test
    @DisplayName("닉네임 변경 실패 - 변경할 닉네임 형식 불일치")
    void updateNickname_fail_invalidNickname() throws Exception {
        // given
        UpdateUserNicknameRequest request = new UpdateUserNicknameRequest("B");

        // when & then
        mockMvc.perform(patch("/api/users/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("닉네임은 2~16자여야 합니다"));
    }


    // ========== 비밀번호 변경 ==========
    @Test
    @DisplayName("비밀번호 변경 성공")
    void updatePassword_success() throws Exception {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(1L, UserRole.BRONZE.name());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        UpdateUserPasswordRequest request = new UpdateUserPasswordRequest("oldPassword123@", "newPassword123@");
        UpdateUserPasswordResponse response = new UpdateUserPasswordResponse(1L, LocalDateTime.now(), LocalDateTime.now());

        given(userService.updatePassword(eq(1L), any(UpdateUserPasswordRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/users/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1L));
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 공백")
    void updatePassword_fail_oldPasswordIsNull() throws Exception {
        // given
        UpdateUserPasswordRequest request = new UpdateUserPasswordRequest(null, "newPassword123@");

        // when & then
        mockMvc.perform(patch("/api/users/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("현재 비밀번호를 입력해주세요"));
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 새 비밀번호 공백")
    void updatePassword_fail_newPasswordIsNull() throws Exception {
        // given
        UpdateUserPasswordRequest request = new UpdateUserPasswordRequest("oldPassword123@", null);

        // when & then
        mockMvc.perform(patch("/api/users/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("새 비밀번호를 입력해주세요"));
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 새 비밀번호 형식 불일치")
    void updatePassword_fail_invalidNewPassword() throws Exception {
        // given
        UpdateUserPasswordRequest request = new UpdateUserPasswordRequest("oldPassword123@", "newPassword");

        // when & then
        mockMvc.perform(patch("/api/users/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("비밀번호 형식이 올바르지 않습니다"));
    }


    // ========== 회원 탈퇴 ==========
    @Test
    @DisplayName("회원 탈퇴 성공")
    void withdraw_success() throws Exception {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(1L, UserRole.BRONZE.name());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        willDoNothing().given(userService).withdraw(1L, "accessToken");

        // when & then
        mockMvc.perform(delete("/api/users/me")
                        .requestAttr("accessToken", "accessToken"))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("refreshToken", 0))
                .andExpect(jsonPath("$.success").value(true));

        verify(userService).withdraw(1L, "accessToken");
    }
}