package com.community.domain.auth.service;

import com.community.common.config.security.JwtProvider;
import com.community.common.exception.ServiceErrorException;
import com.community.domain.auth.dto.request.AuthLoginRequest;
import com.community.domain.auth.dto.response.AuthLoginResponse;
import com.community.domain.auth.exception.AuthExceptionEnum;
import com.community.domain.auth.dto.request.AuthSignupRequest;
import com.community.domain.auth.dto.response.AuthSignupResponse;
import com.community.domain.user.entity.User;
import com.community.domain.user.exception.UserExceptionEnum;
import com.community.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    @Value("${jwt.refreshExpire}")
    private long refreshTokenExpireTime;

    // 회원가입
    public AuthSignupResponse signup(AuthSignupRequest request) {
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new ServiceErrorException(AuthExceptionEnum.DUPLICATED_ID);
        }

        if (userRepository.existsByNickname(request.nickname())) {
            throw new ServiceErrorException(AuthExceptionEnum.DUPLICATED_NICKNAME);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.register(request.loginId(), request.nickname(), encodedPassword);
        userRepository.save(user);

        return new AuthSignupResponse(user.getId(), user.getLoginId(), user.getNickname());
    }

    // 로그인
    public AuthLoginResponse login(AuthLoginRequest request) {
        User user = userRepository.findByLoginIdAndDeletedAtIsNull(request.loginId()).orElseThrow(
                () -> new ServiceErrorException(AuthExceptionEnum.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ServiceErrorException(AuthExceptionEnum.INVALID_CREDENTIALS);
        }

        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getId(),
                refreshToken,
                Duration.ofMillis(refreshTokenExpireTime)
        );

        return new AuthLoginResponse(accessToken, refreshToken);
    }

    // 토큰 재발급
    public AuthLoginResponse reissue(String refreshToken) {
        if (!jwtProvider.validateRefreshToken(refreshToken)) {
            throw new ServiceErrorException(AuthExceptionEnum.INVALID_REFRESH_TOKEN);
        }

        Long userId = jwtProvider.getUserId(refreshToken);
        String oldRefreshToken = (String) redisTemplate.opsForValue()
                .get(REFRESH_TOKEN_PREFIX + userId);

        if (oldRefreshToken == null || !oldRefreshToken.equals(refreshToken)) {
            throw new ServiceErrorException(AuthExceptionEnum.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        String newAccessToken = jwtProvider.createAccessToken(user.getId());
        String newRefreshToken = jwtProvider.createRefreshToken(user.getId());

        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getId(),
                newRefreshToken,
                Duration.ofMillis(refreshTokenExpireTime)
        );

        return new AuthLoginResponse(newAccessToken, newRefreshToken);
    }

    // 로그아웃
    public void logout(Long userId, String accessToken) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);

        long ttl = jwtProvider.getRemainingTtl(accessToken);
        redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + accessToken,
                "logout",
                Duration.ofMillis(ttl)
        );
    }
}
