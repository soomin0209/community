package com.community.domain.auth.service;

import com.community.common.config.security.JwtProvider;
import com.community.common.exception.CommonExceptionEnum;
import com.community.common.exception.ServiceErrorException;
import com.community.domain.auth.dto.request.LoginRequest;
import com.community.domain.auth.dto.response.LoginResponse;
import com.community.domain.auth.exception.AuthExceptionEnum;
import com.community.domain.auth.dto.request.UserSignupRequest;
import com.community.domain.auth.dto.response.SignupResponse;
import com.community.domain.user.entity.User;
import com.community.domain.user.enums.UserType;
import com.community.domain.user.exception.UserExceptionEnum;
import com.community.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import static com.community.common.constant.AppConstants.BLACKLIST_PREFIX;
import static com.community.common.constant.AppConstants.REFRESH_TOKEN_PREFIX;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${jwt.refreshExpire}")
    private long refreshTokenExpireTime;

    // 회원가입
    public SignupResponse signup(UserSignupRequest request) {
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new ServiceErrorException(AuthExceptionEnum.DUPLICATED_ID);
        }

        if (userRepository.existsByNickname(request.nickname())) {
            throw new ServiceErrorException(AuthExceptionEnum.DUPLICATED_NICKNAME);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.register(request.loginId(), request.nickname(), encodedPassword, UserType.USER);
        userRepository.save(user);

        return new SignupResponse(user.getId(), user.getLoginId(), user.getNickname());
    }

    // 로그인
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByLoginIdAndDeletedAtIsNull(request.loginId()).orElseThrow(
                () -> new ServiceErrorException(AuthExceptionEnum.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ServiceErrorException(AuthExceptionEnum.INVALID_CREDENTIALS);
        }

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getType().name());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        try {
            redisTemplate.opsForValue().set(
                    REFRESH_TOKEN_PREFIX + user.getId(),
                    refreshToken,
                    Duration.ofMillis(refreshTokenExpireTime)
            );
        } catch (Exception e) {
            log.error("[AuthService] Redis Refresh Token 저장 실패 - userId={}, msg={}", user.getId(), e.getMessage());
            throw new ServiceErrorException(CommonExceptionEnum.REDIS_CONNECTION_ERROR);
        }

        return new LoginResponse(accessToken, refreshToken);
    }

    // 토큰 재발급
    public LoginResponse reissue(String refreshToken) {
        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            throw new ServiceErrorException(AuthExceptionEnum.INVALID_REFRESH_TOKEN);
        }

        Long userId = jwtProvider.getUserId(refreshToken);
        String oldRefreshToken;
        try {
            oldRefreshToken = (String) redisTemplate.opsForValue()
                    .get(REFRESH_TOKEN_PREFIX + userId);
        } catch (Exception e) {
            log.error("[AuthService] Redis Refresh Token 조회 실패 - userId={}, msg={}", userId, e.getMessage());
            throw new ServiceErrorException(CommonExceptionEnum.REDIS_CONNECTION_ERROR);
        }

        if (oldRefreshToken == null || !oldRefreshToken.equals(refreshToken)) {
            throw new ServiceErrorException(AuthExceptionEnum.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        String newAccessToken = jwtProvider.createAccessToken(user.getId(), user.getType().name());
        String newRefreshToken = jwtProvider.createRefreshToken(user.getId());

        try {
            redisTemplate.opsForValue().set(
                    REFRESH_TOKEN_PREFIX + user.getId(),
                    newRefreshToken,
                    Duration.ofMillis(refreshTokenExpireTime)
            );
        } catch (Exception e) {
            log.error("[AuthService] Redis Refresh Token 갱신 실패 - userId={}, msg={}", user.getId(), e.getMessage());
            throw new ServiceErrorException(CommonExceptionEnum.REDIS_CONNECTION_ERROR);
        }

        return new LoginResponse(newAccessToken, newRefreshToken);
    }

    // 로그아웃
    public void logout(Long userId, String accessToken) {
        long ttl = jwtProvider.getRemainingTtl(accessToken);
        try {
            redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + accessToken,
                    "logout",
                    Duration.ofMillis(ttl)
            );
        } catch (Exception e) {
            log.error("[AuthService] Redis Logout 처리 실패 - userId={}, msg={}", userId, e.getMessage());
            throw new ServiceErrorException(CommonExceptionEnum.REDIS_CONNECTION_ERROR);
        }
    }
}
