package com.community.domain.user.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.user.dto.response.UserGetMineResponse;
import com.community.domain.user.dto.response.UserGetOneResponse;
import com.community.domain.user.entity.User;
import com.community.domain.user.exception.UserExceptionEnum;
import com.community.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    // 프로필 조회
    @Transactional(readOnly = true)
    public UserGetOneResponse getOne(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        return new UserGetOneResponse(
                user.getId(),
                user.getNickname(),
                user.getCreatedAt()
        );
    }

    // 마이페이지 조회
    @Transactional(readOnly = true)
    public UserGetMineResponse getMine(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        return new UserGetMineResponse(
                user.getId(),
                user.getLoginId(),
                user.getNickname(),
                user.getCreatedAt()
        );
    }
}
