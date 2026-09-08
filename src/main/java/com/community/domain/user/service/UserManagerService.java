package com.community.domain.user.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.auth.service.AuthService;
import com.community.domain.user.dto.request.SuspendUserRequest;
import com.community.domain.user.dto.request.UpdateUserRoleRequest;
import com.community.domain.user.dto.response.SuspendUserResponse;
import com.community.domain.user.dto.response.UpdateUserRoleResponse;
import com.community.domain.user.entity.User;
import com.community.domain.user.enums.UserRole;
import com.community.domain.user.exception.UserExceptionEnum;
import com.community.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserManagerService {

    private final UserRepository userRepository;
    private final AuthService authService;

    // 회원 등급 변경
    public UpdateUserRoleResponse updateRole(Long userId, UpdateUserRoleRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        if (user.getRole().getLevel() >= UserRole.MANAGER.getLevel() ||
                request.role().getLevel() >= UserRole.MANAGER.getLevel()) {
            throw new ServiceErrorException(UserExceptionEnum.USER_MODIFICATION_FORBIDDEN);
        }

        user.updateRoleByManager(request.role());
        authService.forceLogout(user.getId());

        return new UpdateUserRoleResponse(
                user.getId(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    // 회원 활동 정지
    public SuspendUserResponse suspend(Long managerId, Long userId, SuspendUserRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        if (user.getRole().getLevel() >= UserRole.MANAGER.getLevel()) {
            throw new ServiceErrorException(UserExceptionEnum.USER_MODIFICATION_FORBIDDEN);
        }

        user.suspendByManager(managerId, request.suspendedReason(), request.suspensionDay());
        authService.forceLogout(user.getId());

        return new SuspendUserResponse(
                user.getId(),
                user.getSuspendedAt(),
                user.getSuspendedReason(),
                user.getSuspensionDay(),
                user.getSuspendedAt().plusDays(user.getSuspensionDay())
        );
    }

    // 회원 강제 탈퇴
    public void withdraw(Long managerId, Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        if (user.getRole().getLevel() >= UserRole.MANAGER.getLevel()) {
            throw new ServiceErrorException(UserExceptionEnum.USER_MODIFICATION_FORBIDDEN);
        }

        user.deleteByManager(managerId);
        authService.forceLogout(user.getId());
    }
}
