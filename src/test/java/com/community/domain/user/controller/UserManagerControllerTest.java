package com.community.domain.user.controller;

import com.community.common.config.security.CustomUserDetails;
import com.community.common.config.web.SuspendedCheckInterceptor;
import com.community.common.config.web.UserVisitInterceptor;
import com.community.common.config.web.WebMvcConfig;
import com.community.domain.user.dto.request.SuspendUserRequest;
import com.community.domain.user.dto.request.UpdateUserRoleRequest;
import com.community.domain.user.dto.response.SuspendUserResponse;
import com.community.domain.user.dto.response.UpdateUserRoleResponse;
import com.community.domain.user.enums.UserRole;
import com.community.domain.user.service.UserManagerService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserManagerController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {WebMvcConfig.class, SuspendedCheckInterceptor.class, UserVisitInterceptor.class}
        )
)
@AutoConfigureMockMvc(addFilters = false)
class UserManagerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserManagerService userManagerService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }


    // ========== 회원 등급 변경 ==========
    @Test
    @DisplayName("회원 등급 변경 성공")
    void updateRole_success() throws Exception {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(1L, UserRole.MANAGER.name());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        UpdateUserRoleRequest request = new UpdateUserRoleRequest(UserRole.SILVER);
        UpdateUserRoleResponse response = new UpdateUserRoleResponse(2L, UserRole.SILVER, LocalDateTime.now(), LocalDateTime.now());

        given(userManagerService.updateRole(eq(1L), eq(2L), any(UpdateUserRoleRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/manager/users/{userId}/role", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(2L))
                .andExpect(jsonPath("$.data.role").value(UserRole.SILVER.name()));
    }

    @Test
    @DisplayName("회원 등급 변경 실패 - 변경할 회원 등급 공백")
    void updateRole_fail_roleIsNull() throws Exception {
        // given
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(null);

        // when & then
        mockMvc.perform(patch("/api/manager/users/{userId}/role", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("변경할 회원 등급을 입력해주세요"));
    }


    // ========== 회원 활동 정지 ==========
    @Test
    @DisplayName("회원 활동 정지 성공")
    void suspend_success() throws Exception {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(1L, UserRole.MANAGER.name());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        SuspendUserRequest request = new SuspendUserRequest("정지 사유", 7, false);
        SuspendUserResponse response = new SuspendUserResponse(2L, LocalDateTime.now(), "정지 사유", 7, LocalDateTime.now().plusDays(7));

        given(userManagerService.suspend(eq(1L), eq(2L), any(SuspendUserRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(patch("/api/manager/users/{userId}/suspend", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(2L))
                .andExpect(jsonPath("$.data.suspendedReason").value("정지 사유"))
                .andExpect(jsonPath("$.data.suspensionDay").value(7));
    }

    @Test
    @DisplayName("회원 활동 정지 실패 - 정지 사유 공백")
    void suspend_fail_suspendedReasonIsNull() throws Exception {
        // given
        SuspendUserRequest request = new SuspendUserRequest(null, 7, false);

        // when & then
        mockMvc.perform(patch("/api/manager/users/{userId}/suspend", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("정지 사유를 입력해주세요"));
    }

    @Test
    @DisplayName("회원 활동 정지 실패 - 정지 기간 양수 아님")
    void suspend_fail_suspensionDayIsNotPositive() throws Exception {
        // given
        SuspendUserRequest request = new SuspendUserRequest("정지 사유", -1, false);

        // when & then
        mockMvc.perform(patch("/api/manager/users/{userId}/suspend", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("정지 기간은 1일 이상이어야 합니다"));
    }


    // ========== 회원 활동 정지 해제 ==========
    @Test
    @DisplayName("회원 활동 정지 해제 성공")
    void unsuspend_success() throws Exception {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(1L, UserRole.MANAGER.name());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        // when & then
        mockMvc.perform(patch("/api/manager/users/{userId}/unsuspend", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userManagerService).unsuspend(1L, 2L);
    }


    // ========== 회원 강제 탈퇴 ==========
    @Test
    @DisplayName("회원 강제 탈퇴 성공")
    void withdraw_success() throws Exception {
        // given
        CustomUserDetails userDetails = new CustomUserDetails(1L, UserRole.MANAGER.name());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

        // when & then
        mockMvc.perform(delete("/api/manager/users/{userId}", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userManagerService).withdraw(1L, 2L);
    }
}