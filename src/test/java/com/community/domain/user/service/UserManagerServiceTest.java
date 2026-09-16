package com.community.domain.user.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.auth.service.AuthService;
import com.community.domain.user.dto.request.SuspendUserRequest;
import com.community.domain.user.dto.request.UpdateUserRoleRequest;
import com.community.domain.user.dto.response.SuspendUserResponse;
import com.community.domain.user.dto.response.UpdateUserRoleResponse;
import com.community.domain.user.entity.User;
import com.community.domain.user.entity.UserSuspension;
import com.community.domain.user.enums.UserRole;
import com.community.domain.user.exception.UserExceptionEnum;
import com.community.domain.user.repository.UserRepository;
import com.community.domain.user.repository.UserSuspensionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserManagerServiceTest {

    @InjectMocks
    private UserManagerService userManagerService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthService authService;

    @Mock
    private UserSuspensionRepository userSuspensionRepository;


    // ========== 회원 등급 변경 ==========
    @Test
    @DisplayName("회원 등급 변경 성공")
    void updateRole_success() {
        // given
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(UserRole.SILVER);

        User manager = User.register("manager", "매니저", "password", UserRole.MANAGER);
        ReflectionTestUtils.setField(manager, "id", 1L);

        User user = User.register("bronze_user", "브론즈유저",  "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 2L);

        given(userRepository.findByIdAndDeletedAtIsNull(manager.getId())).willReturn(Optional.of(manager));
        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));

        // when
        UpdateUserRoleResponse response = userManagerService.updateRole(manager.getId(), user.getId(), request);

        // then
        assertThat(response.userId()).isEqualTo(user.getId());
        assertThat(response.role()).isEqualTo(UserRole.SILVER);
    }

    @Test
    @DisplayName("회원 등급 변경 실패 - 자기보다 높은 권한으로 변경")
    void updateRole_fail_forbidden() {
        // given
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(UserRole.ADMIN);

        User manager = User.register("manager", "매니저", "password", UserRole.MANAGER);
        ReflectionTestUtils.setField(manager, "id", 1L);

        User user = User.register("bronze_user", "브론즈유저",  "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 2L);

        given(userRepository.findByIdAndDeletedAtIsNull(manager.getId())).willReturn(Optional.of(manager));
        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> userManagerService.updateRole(manager.getId(), user.getId(), request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserExceptionEnum.USER_MODIFICATION_FORBIDDEN.getMessage());
    }


    // ========== 회원 활동 정지 ==========
    @Test
    @DisplayName("회원 활동 정지 성공")
    void suspend_success() {
        // given
        SuspendUserRequest request = new SuspendUserRequest("욕설", 3, false);

        User manager = User.register("manager", "매니저", "password", UserRole.MANAGER);
        ReflectionTestUtils.setField(manager, "id", 1L);

        User user = User.register("bronze_user", "브론즈유저",  "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 2L);

        UserSuspension suspension = UserSuspension.temporarilySuspend(user, manager, request.suspendedReason(), request.suspensionDay(), LocalDateTime.now());

        given(userRepository.findByIdAndDeletedAtIsNull(manager.getId())).willReturn(Optional.of(manager));
        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(userSuspensionRepository.save(any(UserSuspension.class))).willReturn(suspension);

        // when
        SuspendUserResponse response = userManagerService.suspend(manager.getId(), user.getId(), request);

        // then
        assertThat(response.userId()).isEqualTo(user.getId());
        assertThat(response.suspendedReason()).isEqualTo(request.suspendedReason());
        assertThat(response.suspensionDay()).isEqualTo(request.suspensionDay());
        assertThat(user.isSuspended()).isTrue();
    }

    @Test
    @DisplayName("회원 활동 정지 성공 - 영구 정지")
    void suspend_success_permanently() {
        // given
        SuspendUserRequest request = new SuspendUserRequest("신고 누적", null, true);

        User manager = User.register("manager", "매니저", "password", UserRole.MANAGER);
        ReflectionTestUtils.setField(manager, "id", 1L);

        User user = User.register("bronze_user", "브론즈유저",  "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 2L);

        UserSuspension suspension = UserSuspension.permanentlySuspend(user, manager, request.suspendedReason(), LocalDateTime.now());

        given(userRepository.findByIdAndDeletedAtIsNull(manager.getId())).willReturn(Optional.of(manager));
        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(userSuspensionRepository.save(any(UserSuspension.class))).willReturn(suspension);

        // when
        SuspendUserResponse response = userManagerService.suspend(manager.getId(), user.getId(), request);

        // then
        assertThat(response.userId()).isEqualTo(user.getId());
        assertThat(response.suspendedReason()).isEqualTo(request.suspendedReason());
        assertThat(response.suspendedUntil()).isEqualTo(LocalDateTime.of(9999, 12, 31, 23, 59, 59));
        assertThat(user.isSuspended()).isTrue();
    }


    // ========== 회원 활동 정지 해제 ==========
    @Test
    @DisplayName("회원 활동 정지 해제 성공")
    void unsuspend_success() {

    }
}