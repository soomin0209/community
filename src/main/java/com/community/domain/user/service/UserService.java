package com.community.domain.user.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.auth.exception.AuthExceptionEnum;
import com.community.domain.post.repository.PostRepository;
import com.community.domain.user.dto.request.UserUpdateNicknameRequest;
import com.community.domain.user.dto.request.UserUpdatePasswordRequest;
import com.community.domain.user.dto.response.UserGetMineResponse;
import com.community.domain.user.dto.response.UserGetOneResponse;
import com.community.domain.user.dto.response.UserUpdateNicknameResponse;
import com.community.domain.user.dto.response.UserUpdatePasswordResponse;
import com.community.domain.user.entity.User;
import com.community.domain.user.exception.UserExceptionEnum;
import com.community.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;

    // 프로필 조회
    @Transactional(readOnly = true)
    public UserGetOneResponse getOne(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        Long postCount = postRepository.countByUserIdAndDeletedAtIsNull(userId);

        return new UserGetOneResponse(
                user.getId(),
                user.getNickname(),
                user.getCreatedAt(),
                postCount
        );
    }

    // 마이페이지 조회
    @Transactional(readOnly = true)
    public UserGetMineResponse getMine(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        Long postCount = postRepository.countByUserIdAndDeletedAtIsNull(userId);

        return new UserGetMineResponse(
                user.getId(),
                user.getLoginId(),
                user.getNickname(),
                user.getCreatedAt(),
                postCount
        );
    }

    // 닉네임 변경
    public UserUpdateNicknameResponse updateNickname(Long userId, UserUpdateNicknameRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        if (user.getNickname().equals(request.nickname())) {
            throw new ServiceErrorException(UserExceptionEnum.NICKNAME_UNCHANGED);
        }

        if (userRepository.existsByNickname(request.nickname())) {
            throw new ServiceErrorException(AuthExceptionEnum.DUPLICATED_NICKNAME);
        }

        user.updateNickname(request.nickname());

        return new UserUpdateNicknameResponse(
                user.getId(),
                user.getNickname(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    // 비밀번호 변경
    public UserUpdatePasswordResponse updatePassword(Long userId, UserUpdatePasswordRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new ServiceErrorException(UserExceptionEnum.PASSWORD_MISMATCH);
        }

        if(passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new ServiceErrorException(UserExceptionEnum.PASSWORD_UNCHANGED);
        }

        String encodedPassword = passwordEncoder.encode(request.newPassword());
        user.updatePassword(encodedPassword);

        return new UserUpdatePasswordResponse(
                user.getId(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    // 회원 탈퇴
    public void withdraw(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        user.delete();
    }
}
