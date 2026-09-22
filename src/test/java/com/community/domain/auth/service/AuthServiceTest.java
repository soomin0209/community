package com.community.domain.auth.service;

import com.community.common.config.security.JwtProvider;
import com.community.common.exception.CommonExceptionEnum;
import com.community.common.exception.ServiceErrorException;
import com.community.domain.auth.dto.request.LoginRequest;
import com.community.domain.auth.dto.request.UserSignupRequest;
import com.community.domain.auth.dto.response.LoginResponse;
import com.community.domain.auth.dto.response.SignupResponse;
import com.community.domain.auth.exception.AuthExceptionEnum;
import com.community.domain.user.entity.User;
import com.community.domain.user.enums.UserRole;
import com.community.domain.user.exception.UserExceptionEnum;
import com.community.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static com.community.common.constant.AppConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "accessTokenExpireTime", 1800000L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpireTime", 604800000L);
    }


    // ========== 회원가입 ==========
    @Test
    @DisplayName("회원가입 성공")
    void signup_success() {
        // given
        UserSignupRequest request = new UserSignupRequest("bronze_user", "브론즈유저", "password");
        User user = User.register(request.loginId(), request.nickname(), "encodedPassword", UserRole.BRONZE);

        given(userRepository.existsByLoginId(request.loginId())).willReturn(false);
        given(userRepository.existsByNickname(request.nickname())).willReturn(false);
        given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willReturn(user);

        // when
        SignupResponse response = authService.signup(request);

        // then
        assertThat(response.loginId()).isEqualTo("bronze_user");
        assertThat(response.nickname()).isEqualTo("브론즈유저");
    }

    @Test
    @DisplayName("회원가입 실패 - 아이디 중복")
    void signup_fail_duplicateLoginId() {
        // given
        UserSignupRequest request = new UserSignupRequest("bronze_user", "브론즈유저", "password");

        given(userRepository.existsByLoginId(request.loginId())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuthExceptionEnum.DUPLICATED_ID.getMessage());
    }

    @Test
    @DisplayName("회원가입 실패 - 닉네임 중복")
    void signup_fail_duplicateNickname() {
        // given
        UserSignupRequest request = new UserSignupRequest("bronze_user", "브론즈유저", "password");

        given(userRepository.existsByLoginId(request.loginId())).willReturn(false);
        given(userRepository.existsByNickname(request.nickname())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuthExceptionEnum.DUPLICATED_NICKNAME.getMessage());
    }


    // ========== 로그인 ==========
    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        // given
        LoginRequest request = new LoginRequest("bronze_user", "password");
        User user = User.register(request.loginId(), "브론즈유저", "encodedPassword", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findByLoginIdAndDeletedAtIsNull(request.loginId())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(true);
        given(jwtProvider.createAccessToken(user.getId(), user.getRole().name())).willReturn("accessToken");
        given(jwtProvider.createRefreshToken(user.getId())).willReturn("refreshToken");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        LoginResponse response = authService.login(request);

        // then
        assertThat(response.accessToken()).isEqualTo("accessToken");
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 아이디")
    void login_fail_invalidLoginId() {
        // given
        LoginRequest request = new LoginRequest("bronze_user", "password");

        given(userRepository.findByLoginIdAndDeletedAtIsNull(request.loginId())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuthExceptionEnum.INVALID_CREDENTIALS.getMessage());
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_fail_invalidPassword() {
        // given
        LoginRequest request = new LoginRequest("bronze_user", "password");
        User user = User.register(request.loginId(), "브론즈유저", "encodedPassword", UserRole.BRONZE);

        given(userRepository.findByLoginIdAndDeletedAtIsNull(request.loginId())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuthExceptionEnum.INVALID_CREDENTIALS.getMessage());
    }

    @Test
    @DisplayName("로그인 실패 - Redis 토큰 저장 실패")
    void login_fail_redisConnectionError() {
        // given
        LoginRequest request = new LoginRequest("bronze_user", "password");
        User user = User.register(request.loginId(), "브론즈유저", "encodedPassword", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        given(userRepository.findByLoginIdAndDeletedAtIsNull(request.loginId())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(request.password(), user.getPassword())).willReturn(true);
        given(jwtProvider.createAccessToken(user.getId(), user.getRole().name())).willReturn("accessToken");
        given(jwtProvider.createRefreshToken(user.getId())).willReturn("refreshToken");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        willThrow(new RuntimeException("Redis connection failed"))
                .given(valueOperations)
                .set(eq(REFRESH_TOKEN_PREFIX + user.getId()), eq("refreshToken"), any(Duration.class));

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CommonExceptionEnum.REDIS_CONNECTION_ERROR.getMessage());
    }


    // ========== 토큰 재발급 ==========
    @Test
    @DisplayName("토큰 재발급 성공")
    void reissue_success() {
        // given
        String refreshToken = "refreshToken";
        User user = User.register("bronze_user", "브론즈유저", "encodedPassword", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        given(jwtProvider.validateRefreshToken(refreshToken)).willReturn(true);
        given(jwtProvider.getUserId(refreshToken)).willReturn(user.getId());
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(REFRESH_TOKEN_PREFIX + user.getId())).willReturn(refreshToken);
        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(jwtProvider.createAccessToken(user.getId(), user.getRole().name())).willReturn("newAccessToken");
        given(jwtProvider.createRefreshToken(user.getId())).willReturn("newRefreshToken");

        // when
        LoginResponse response = authService.reissue(refreshToken);

        // then
        assertThat(response.accessToken()).isEqualTo("newAccessToken");
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 유효하지 않는 토큰")
    void reissue_fail_invalidRefreshToken() {
        // given
        String refreshToken = "refreshToken";

        given(jwtProvider.validateRefreshToken(refreshToken)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuthExceptionEnum.INVALID_REFRESH_TOKEN.getMessage());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - Redis 토큰 조회 실패")
    void reissue_fail_redisConnectionError() {
        // given
        String refreshToken = "refreshToken";
        Long userId = 1L;

        given(jwtProvider.validateRefreshToken(refreshToken)).willReturn(true);
        given(jwtProvider.getUserId(refreshToken)).willReturn(userId);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(REFRESH_TOKEN_PREFIX + userId)).willThrow(new RuntimeException("Redis connection failed"));

        // when & then
        assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CommonExceptionEnum.REDIS_CONNECTION_ERROR.getMessage());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - Redis에 저장된 토큰 없음")
    void reissue_fail_redisTokenIsNull() {
        // given
        String refreshToken = "refreshToken";
        Long userId = 1L;

        given(jwtProvider.validateRefreshToken(refreshToken)).willReturn(true);
        given(jwtProvider.getUserId(refreshToken)).willReturn(userId);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(REFRESH_TOKEN_PREFIX + userId)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuthExceptionEnum.INVALID_REFRESH_TOKEN.getMessage());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - Redis에 저장된 토큰과 불일치")
    void reissue_fail_redisTokenMismatch() {
        // given
        String refreshToken = "refreshToken";
        Long userId = 1L;

        given(jwtProvider.validateRefreshToken(refreshToken)).willReturn(true);
        given(jwtProvider.getUserId(refreshToken)).willReturn(userId);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(REFRESH_TOKEN_PREFIX + userId)).willReturn("differentRefreshToken");

        // when & then
        assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(AuthExceptionEnum.INVALID_REFRESH_TOKEN.getMessage());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 탈퇴 사용자")
    void reissue_fail_userNotFound() {
        // given
        String refreshToken = "refreshToken";
        Long userId = 1L;

        given(jwtProvider.validateRefreshToken(refreshToken)).willReturn(true);
        given(jwtProvider.getUserId(refreshToken)).willReturn(userId);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(REFRESH_TOKEN_PREFIX + userId)).willReturn(refreshToken);
        given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(UserExceptionEnum.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("토큰 재발급 실패 - Redis 토큰 갱신 실패")
    void reissue_fail_redisTokenUpdateFail() {
        // given
        String refreshToken = "refreshToken";
        User user = User.register("bronze_user", "브론즈유저", "encodedPassword", UserRole.BRONZE);
        ReflectionTestUtils.setField(user, "id", 1L);

        given(jwtProvider.validateRefreshToken(refreshToken)).willReturn(true);
        given(jwtProvider.getUserId(refreshToken)).willReturn(user.getId());
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(REFRESH_TOKEN_PREFIX + user.getId())).willReturn(refreshToken);
        given(userRepository.findByIdAndDeletedAtIsNull(user.getId())).willReturn(Optional.of(user));
        given(jwtProvider.createAccessToken(user.getId(), user.getRole().name())).willReturn("newAccessToken");
        given(jwtProvider.createRefreshToken(user.getId())).willReturn("newRefreshToken");
        willThrow(new RuntimeException("Redis connection failed"))
                .given(valueOperations)
                .set(eq(REFRESH_TOKEN_PREFIX + user.getId()), eq("newRefreshToken"), any(Duration.class));

        // when & then
        assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CommonExceptionEnum.REDIS_CONNECTION_ERROR.getMessage());
    }


    // ========== 로그아웃 ==========
    @Test
    @DisplayName("로그아웃 성공")
    void logout_success() {
        // given
        Long userId = 1L;
        String accessToken = "accessToken";
        long remainingTtl = 1000L;

        given(jwtProvider.getRemainingTtl(accessToken)).willReturn(remainingTtl);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        authService.logout(userId, accessToken);

        // then
        verify(redisTemplate).delete(REFRESH_TOKEN_PREFIX + userId);
        verify(valueOperations).set(
                BLACKLIST_PREFIX + accessToken,
                "logout",
                Duration.ofMillis(remainingTtl)
        );
    }

    @Test
    @DisplayName("로그아웃 실패 - Redis 토큰 삭제 실패")
    void logout_fail_redisTokenDeleteFail() {
        // given
        Long userId = 1L;
        String accessToken = "accessToken";
        long remainingTtl = 1000L;

        given(jwtProvider.getRemainingTtl(accessToken)).willReturn(remainingTtl);
        given(redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId)).willThrow(new RuntimeException("Redis connection failed"));

        // when & then
        assertThatThrownBy(() -> authService.logout(userId, accessToken))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CommonExceptionEnum.REDIS_CONNECTION_ERROR.getMessage());
    }


    // ========== 회원 탈퇴 ==========
    @Test
    @DisplayName("회원 탈퇴 성공")
    void withdraw_success() {
        // given
        Long userId = 1L;
        String accessToken = "accessToken";
        long remainingTtl = 1000L;

        given(jwtProvider.getRemainingTtl(accessToken)).willReturn(remainingTtl);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        authService.withdraw(userId, accessToken);

        // then
        verify(redisTemplate).delete(REFRESH_TOKEN_PREFIX + userId);
        verify(valueOperations).set(
                BLACKLIST_PREFIX + accessToken,
                "withdraw",
                Duration.ofMillis(remainingTtl)
        );
        verify(valueOperations).set(
                BLACKLIST_ALL_PREFIX + userId,
                "withdraw",
                Duration.ofMillis(1800000L)
        );
    }

    @Test
    @DisplayName("회원 탈퇴 실패 - Redis 토큰 삭제 실패")
    void withdraw_fail_redisTokenDeleteFail() {
        // given
        Long userId = 1L;
        String accessToken = "accessToken";
        long remainingTtl = 1000L;

        given(jwtProvider.getRemainingTtl(accessToken)).willReturn(remainingTtl);
        given(redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId)).willThrow(new RuntimeException("Redis connection failed"));

        // when & then
        assertThatThrownBy(() -> authService.withdraw(userId, accessToken))
                .isInstanceOf(ServiceErrorException.class)
                .hasMessage(CommonExceptionEnum.REDIS_CONNECTION_ERROR.getMessage());
    }


    // ========== 권한 변경용 토큰 만료 ==========
    @Test
    @DisplayName("권한 변경용 토큰 만료 성공")
    void invalidateAccessToken_success() {
        // given
        Long userId = 1L;

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        authService.invalidateAccessToken(userId);

        // then
        verify(valueOperations).set(
                BLACKLIST_ALL_PREFIX + userId,
                "update_role",
                Duration.ofMillis(1800000L)
        );
    }

    @Test
    @DisplayName("권한 변경용 토큰 만료 실패 - Redis 장애")
    void invalidateAccessToken_fail_redisConnectionError() {
        // given
        Long userId = 1L;

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        willThrow(new RuntimeException("Redis connection failed"))
                .given(valueOperations)
                .set(eq(BLACKLIST_ALL_PREFIX + userId), eq("true"), any(Duration.class));

        // when & then
        authService.invalidateAccessToken(userId);
    }


    // ========== 강제 탈퇴용 토큰 만료 ==========
    @Test
    @DisplayName("강제 탈퇴용 토큰 만료 성공")
    void invalidateAllTokens_success() {
        // given
        Long userId = 1L;

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        authService.invalidateAllTokens(userId);

        // then
        verify(redisTemplate).delete(REFRESH_TOKEN_PREFIX + userId);
        verify(valueOperations).set(
                BLACKLIST_ALL_PREFIX + userId,
                "force_withdraw",
                Duration.ofMillis(1800000L)
        );
    }

    @Test
    @DisplayName("강제 탈퇴용 토큰 만료 실패 - Redis 장애")
    void invalidateAllTokens_fail_redisConnectionError() {
        // given
        Long userId = 1L;

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        willThrow(new RuntimeException("Redis connection failed"))
                .given(valueOperations)
                .set(eq(BLACKLIST_ALL_PREFIX + userId), eq("true"), any(Duration.class));

        // when & then
        authService.invalidateAllTokens(userId);
    }
}