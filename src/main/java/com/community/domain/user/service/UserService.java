package com.community.domain.user.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.auth.exception.AuthExceptionEnum;
import com.community.domain.post.repository.PostRepository;
import com.community.domain.user.dto.request.UpdateUserNicknameRequest;
import com.community.domain.user.dto.request.UpdateUserPasswordRequest;
import com.community.domain.user.dto.response.GetMypageResponse;
import com.community.domain.user.dto.response.GetOneUserResponse;
import com.community.domain.user.dto.response.UpdateUserNicknameResponse;
import com.community.domain.user.dto.response.UpdateUserPasswordResponse;
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
    public GetOneUserResponse getOne(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        return new GetOneUserResponse(
                user.getId(),
                user.getNickname(),
                user.getCreatedAt(),
                user.getVisitCount(),
                user.getPostCount(),
                user.getCommentCount()
        );
    }

    // 마이페이지 조회
    @Transactional(readOnly = true)
    public GetMypageResponse getMine(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        return new GetMypageResponse(
                user.getId(),
                user.getLoginId(),
                user.getNickname(),
                user.getCreatedAt(),
                user.getVisitCount(),
                user.getPostCount(),
                user.getCommentCount()
        );
    }

    // 닉네임 변경
    public UpdateUserNicknameResponse updateNickname(Long userId, UpdateUserNicknameRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        if (user.getNickname().equals(request.nickname())) {
            throw new ServiceErrorException(UserExceptionEnum.NICKNAME_UNCHANGED);
        }

        if (userRepository.existsByNickname(request.nickname())) {
            throw new ServiceErrorException(AuthExceptionEnum.DUPLICATED_NICKNAME);
        }

        user.updateNickname(request.nickname());

        return new UpdateUserNicknameResponse(
                user.getId(),
                user.getNickname(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    // 비밀번호 변경
    public UpdateUserPasswordResponse updatePassword(Long userId, UpdateUserPasswordRequest request) {
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

        return new UpdateUserPasswordResponse(
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
