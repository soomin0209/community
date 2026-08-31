package com.community.domain.auth.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.auth.dto.request.AdminSignupRequest;
import com.community.domain.auth.dto.response.SignupResponse;
import com.community.domain.auth.exception.AuthExceptionEnum;
import com.community.domain.user.entity.User;
import com.community.domain.user.enums.UserType;
import com.community.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.key}")
    private String adminKey;

    // 관리자 회원가입
    public SignupResponse signup(AdminSignupRequest request) {
        if (!adminKey.equals(request.adminKey())) {
            throw new ServiceErrorException(AuthExceptionEnum.INVALID_ADMIN_KEY);
        }

        if (userRepository.existsByLoginId(request.loginId())) {
            throw new ServiceErrorException(AuthExceptionEnum.DUPLICATED_ID);
        }

        if (userRepository.existsByNickname(request.nickname())) {
            throw new ServiceErrorException(AuthExceptionEnum.DUPLICATED_NICKNAME);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.register(request.loginId(), request.nickname(), encodedPassword, UserType.ADMIN);
        userRepository.save(user);

        return new SignupResponse(user.getId(), user.getLoginId(), user.getNickname());
    }
}
