package com.community.domain.user.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.auth.exception.AuthExceptionEnum;
import com.community.domain.auth.service.AuthService;
import com.community.domain.user.dto.projection.UserSuspensionProjection;
import com.community.domain.user.dto.request.UpdateUserNicknameRequest;
import com.community.domain.user.dto.request.UpdateUserPasswordRequest;
import com.community.domain.user.dto.response.*;
import com.community.domain.user.entity.User;
import com.community.domain.user.entity.UserSuspension;
import com.community.domain.user.exception.UserExceptionEnum;
import com.community.domain.user.repository.UserRepository;
import com.community.domain.user.repository.UserSuspensionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final UserSuspensionRepository userSuspensionRepository;

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
                user.getCommentCount(),
                user.getRole()
        );
    }

    // 마이페이지 조회
    @Transactional(readOnly = true)
    public GetMypageResponse getMine(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        List<UserSuspensionProjection> suspensionProjections = new ArrayList<>();
        LocalDateTime suspendedUntil = null;

        if (user.isSuspended()) {
            List<UserSuspension> suspensionList = userSuspensionRepository.findAllByUserIdAndUnsuspendedAtIsNull(user.getId());
            for (UserSuspension suspension : suspensionList) {
                UserSuspensionProjection suspensionProjection = new UserSuspensionProjection(
                        user.getId(),
                        suspension.getReason(),
                        suspension.getDay(),
                        suspension.getSuspendedAt()
                );
                suspensionProjections.add(suspensionProjection);
            }
            suspendedUntil = user.getSuspendedUntil();
        }

        return new GetMypageResponse(
                user.getId(),
                user.getLoginId(),
                user.getNickname(),
                user.getCreatedAt(),
                user.getVisitCount(),
                user.getPostCount(),
                user.getCommentCount(),
                user.getRole(),
                suspensionProjections,
                suspendedUntil
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
    public void withdraw(Long userId, String accessToken) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        user.delete();
        authService.withdraw(userId, accessToken);
    }
}
