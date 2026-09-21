package com.community.domain.auth.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.auth.dto.request.AdminSignupRequest;
import com.community.domain.auth.dto.response.SignupResponse;
import com.community.domain.auth.exception.AuthExceptionEnum;
import com.community.domain.user.entity.User;
import com.community.domain.user.enums.UserRole;
import com.community.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthAdminServiceTest {

    @InjectMocks
    private AuthAdminService authAdminService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authAdminService, "adminKey", "adminsecretkey");
    }


    // ========== 관리자 회원가입 ==========
    @Test
    @DisplayName("관리자 회원가입 성공")
    void signup_success() {
        // given
        AdminSignupRequest request = new AdminSignupRequest("admin", "관리자", "password", "adminsecretkey");
        User user = User.register(request.loginId(), request.nickname(), "encodedPassword", UserRole.ADMIN);

        given(userRepository.existsByLoginId(request.loginId())).willReturn(false);
        given(userRepository.existsByNickname(request.nickname())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willReturn(user);

        // when
        SignupResponse response = authAdminService.signup(request);

        // then
        assertThat(response.loginId()).isEqualTo("admin");
        assertThat(response.nickname()).isEqualTo("관리자");
    }

    @Test
    @DisplayName("관리자 회원가입 실패 - adminKey 불일치")
    void signup_fail_invalidAdminKey() {
        // given
        AdminSignupRequest request = new AdminSignupRequest("admin", "관리자", "password", "wrongAdminsecretkey");

        // when & then
        assertThatThrownBy(() -> authAdminService.signup(request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuthExceptionEnum.INVALID_ADMIN_KEY.getMessage());
    }

    @Test
    @DisplayName("관리자 회원가입 실패 - 아이디 중복")
    void signup_fail_duplicateLoginId() {
        // given
        AdminSignupRequest request = new AdminSignupRequest("admin", "관리자", "password", "adminsecretkey");

        given(userRepository.existsByLoginId(request.loginId())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authAdminService.signup(request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuthExceptionEnum.DUPLICATED_ID.getMessage());
    }

    @Test
    @DisplayName("관리자 회원가입 실패 - 닉네임 중복")
    void signup_fail_duplicateNickname() {
        // given
        AdminSignupRequest request = new AdminSignupRequest("admin", "관리자", "password", "adminsecretkey");

        given(userRepository.existsByLoginId(request.loginId())).willReturn(false);
        given(userRepository.existsByNickname(request.nickname())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authAdminService.signup(request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuthExceptionEnum.DUPLICATED_NICKNAME.getMessage());
    }
}