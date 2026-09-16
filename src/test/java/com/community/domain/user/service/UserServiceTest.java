package com.community.domain.user.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.auth.exception.AuthExceptionEnum;
import com.community.domain.auth.service.AuthService;
import com.community.domain.user.dto.request.UpdateUserNicknameRequest;
import com.community.domain.user.dto.request.UpdateUserPasswordRequest;
import com.community.domain.user.dto.response.GetMypageResponse;
import com.community.domain.user.dto.response.GetOneUserResponse;
import com.community.domain.user.dto.response.UpdateUserNicknameResponse;
import com.community.domain.user.dto.response.UpdateUserPasswordResponse;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthService authService;

    @Mock
    private UserSuspensionRepository userSuspensionRepository;


    // ========== 프로필 조회 ==========
    @Test
    @DisplayName("프로필 조회 성공")
    void getOne_success() {
        // given
        User user = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));

        // when
        GetOneUserResponse response = userService.getOne(user.getId());

        // then
        assertThat(response.nickname()).isEqualTo("브론즈유저");
        assertThat(response.role().name()).isEqualTo(UserRole.BRONZE.name());
    }

    @Test
    @DisplayName("프로필 조회 실패 - 사용자 없음")
    void getOne_fail_userNotFound() {
        // given
        Long userId = 99L;

        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getOne(userId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserExceptionEnum.USER_NOT_FOUND.getMessage());
    }


    // ========== 마이페이지 조회 ==========
    @Test
    @DisplayName("마이페이지 조회 성공")
    void getMine_success() {
        // given
        User user = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));

        // when
        GetMypageResponse response = userService.getMine(user.getId());

        // then
        assertThat(response.nickname()).isEqualTo("브론즈유저");
        assertThat(response.role().name()).isEqualTo(UserRole.BRONZE.name());
    }

    @Test
    @DisplayName("마이페이지 조회 성공 - 정지된 사용자")
    void getMine_success_suspended() {
        // given
        User user = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        User manager = User.register("manager", "매니저", "password", UserRole.MANAGER);
        ReflectionTestUtils.setField(manager, "id", 2L);

        UserSuspension suspension1 = UserSuspension.temporarilySuspend(user, manager, "욕설", 3, LocalDateTime.now());
        UserSuspension suspension2 = UserSuspension.temporarilySuspend(user, manager, "신고", 7, LocalDateTime.now());
        ReflectionTestUtils.setField(user, "suspendedUntil", LocalDateTime.now().plusDays(10));

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(userSuspensionRepository.findAllByUserIdAndUnsuspendedAtIsNull(user.getId())).willReturn(List.of(suspension1, suspension2));

        // when
        GetMypageResponse response = userService.getMine(user.getId());

        // then
        assertThat(response.nickname()).isEqualTo("브론즈유저");
        assertThat(response.role().name()).isEqualTo(UserRole.BRONZE.name());
        assertThat(response.suspensions()).hasSize(2);
    }

    @Test
    @DisplayName("마이페이지 조회 실패 - 사용자 없음")
    void getMine_fail_userNotFound() {
        // given
        Long userId = 99L;

        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getMine(userId))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserExceptionEnum.USER_NOT_FOUND.getMessage());
    }


    // ========== 닉네임 변경 ==========
    @Test
    @DisplayName("닉네임 변경 성공")
    void updateNickname_success() {
        // given
        UpdateUserNicknameRequest request = new UpdateUserNicknameRequest("브론즈");
        User user = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(userRepository.existsByNickname(request.nickname())).willReturn(false);

        // when
        UpdateUserNicknameResponse response = userService.updateNickname(user.getId(), request);

        // then
        assertThat(response.nickname()).isEqualTo("브론즈");
    }

    @Test
    @DisplayName("닉네임 변경 실패 - 사용자 없음")
    void updateNickname_fail_userNotFound() {
        // given
        UpdateUserNicknameRequest request = new UpdateUserNicknameRequest("브론즈");
        Long userId = 99L;

        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.updateNickname(userId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserExceptionEnum.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("닉네임 변경 실패 - 기존 닉네임과 동일")
    void updateNickname_fail_isSame() {
        // given
        UpdateUserNicknameRequest request = new UpdateUserNicknameRequest("브론즈유저");
        User user = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> userService.updateNickname(user.getId(), request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserExceptionEnum.NICKNAME_UNCHANGED.getMessage());
    }

    @Test
    @DisplayName("닉네임 변경 실패 - 닉네임 중복")
    void updateNickname_fail_duplicate() {
        // given
        UpdateUserNicknameRequest request = new UpdateUserNicknameRequest("브론즈");
        User user = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(userRepository.existsByNickname(request.nickname())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.updateNickname(user.getId(), request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuthExceptionEnum.DUPLICATED_NICKNAME.getMessage());
    }


    // ========== 비밀번호 변경 ==========
    @Test
    @DisplayName("비밀번호 변경 성공")
    void updatePassword_success() {
        // given
        UpdateUserPasswordRequest request = new UpdateUserPasswordRequest("oldPassword", "newPassword");
        User user = User.register("bronze_user", "브론즈유저", "encodedOldPassword", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.oldPassword(), user.getPassword())).willReturn(true);
        given(passwordEncoder.matches(request.newPassword(), user.getPassword())).willReturn(false);
        given(passwordEncoder.encode(request.newPassword())).willReturn("encodedNewPassword");

        // when
        UpdateUserPasswordResponse response = userService.updatePassword(user.getId(), request);

        // then
        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 사용자 없음")
    void updatePassword_fail_userNotFound() {
        // given
        UpdateUserPasswordRequest request = new UpdateUserPasswordRequest("oldPassword", "newPassword");
        Long userId = 99L;

        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.updatePassword(userId, request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserExceptionEnum.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 기존 비밀번호 불일치")
    void updatePassword_fail_mismatchOldPassword() {
        // given
        UpdateUserPasswordRequest request = new UpdateUserPasswordRequest("wrongOldPassword", "newPassword");
        User user = User.register("bronze_user", "브론즈유저", "encodedOldPassword", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.oldPassword(), user.getPassword())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> userService.updatePassword(user.getId(), request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserExceptionEnum.PASSWORD_MISMATCH.getMessage());
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 새 비밀번호가 기존 비밀번호와 동일")
    void updatePassword_fail_newPasswordIsSame() {
        // given
        UpdateUserPasswordRequest request = new UpdateUserPasswordRequest("oldPassword", "oldPassword");
        User user = User.register("bronze_user", "브론즈유저", "encodedOldPassword", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.oldPassword(), user.getPassword())).willReturn(true);
        given(passwordEncoder.matches(request.newPassword(), user.getPassword())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.updatePassword(user.getId(), request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserExceptionEnum.PASSWORD_UNCHANGED.getMessage());
    }


    // ========== 회원 탈퇴 ==========
    @Test
    @DisplayName("회원 탈퇴 성공")
    void withdraw_success() {
        // given
        User user = User.register("bronze_user", "브론즈유저", "password", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);
        String accessToken = "accessToken";

        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));

        // when
        userService.withdraw(user.getId(), accessToken);

        // then
        assertThat(user.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - 사용자 없음")
    void withdraw_fail_userNotFound() {
        // given
        Long userId = 99L;
        String accessToken = "accessToken";

        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.withdraw(userId, accessToken))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserExceptionEnum.USER_NOT_FOUND.getMessage());
    }
}