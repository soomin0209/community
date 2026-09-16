package com.community.domain.user.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.auth.service.AuthService;
import com.community.domain.user.dto.request.SuspendUserRequest;
import com.community.domain.user.dto.request.UpdateUserRoleRequest;
import com.community.domain.user.dto.response.SuspendUserResponse;
import com.community.domain.user.dto.response.UpdateUserRoleResponse;
import com.community.domain.user.entity.User;
import com.community.domain.user.entity.UserSuspension;
import com.community.domain.user.exception.UserExceptionEnum;
import com.community.domain.user.repository.UserRepository;
import com.community.domain.user.repository.UserSuspensionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserManagerService {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final UserSuspensionRepository userSuspensionRepository;

    // 회원 등급 변경
    public UpdateUserRoleResponse updateRole(Long managerId, Long userId, UpdateUserRoleRequest request) {
        ManagerAndUser managerAndUser = validateManagerAndUser(managerId, userId);
        User manager = managerAndUser.manager();
        User user = managerAndUser.user();

        if (request.role().getLevel() >= manager.getRole().getLevel()) {
            throw new ServiceErrorException(UserExceptionEnum.USER_MODIFICATION_FORBIDDEN);
        }

        user.updateRoleByManager(request.role());
        authService.invalidateAccessToken(user.getId());

        return new UpdateUserRoleResponse(
                user.getId(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    // 회원 활동 정지
    public SuspendUserResponse suspend(Long managerId, Long userId, SuspendUserRequest request) {
        ManagerAndUser managerAndUser = validateManagerAndUser(managerId, userId);
        User manager = managerAndUser.manager();
        User user = managerAndUser.user();

        LocalDateTime now = LocalDateTime.now();

        UserSuspension suspension;
        if (request.isPermanent()) {
            suspension = UserSuspension.permanentlySuspend(user, manager, request.suspendedReason(), now);
        } else  {
            suspension = UserSuspension.temporarilySuspend(user, manager, request.suspendedReason(), request.suspensionDay(), now);
        }
        userSuspensionRepository.save(suspension);

        user.updateSuspendedUntil(now, request.suspensionDay(), request.isPermanent());

        return new SuspendUserResponse(
                user.getId(),
                suspension.getSuspendedAt(),
                suspension.getReason(),
                suspension.getDay(),
                user.getSuspendedUntil()
        );
    }

    // 회원 활동 정지 해제
    public void unsuspend(Long managerId, Long userId) {
        ManagerAndUser managerAndUser = validateManagerAndUser(managerId, userId);
        User manager = managerAndUser.manager();
        User user = managerAndUser.user();

        if (!user.isSuspended()) {
            throw new ServiceErrorException(UserExceptionEnum.USER_NOT_SUSPENDED);
        }

        LocalDateTime now = LocalDateTime.now();

        List<UserSuspension> suspensionList = userSuspensionRepository.findAllByUserIdAndUnsuspendedAtIsNull(user.getId());
        for (UserSuspension suspension : suspensionList) {
            suspension.unsuspend(now, manager);
        }

        user.resetSuspendedUntil();
    }

    // 회원 강제 탈퇴
    public void withdraw(Long managerId, Long userId) {
        ManagerAndUser managerAndUser = validateManagerAndUser(managerId, userId);
        User manager = managerAndUser.manager();
        User user = managerAndUser.user();

        user.deleteByManager(manager.getId());
        authService.invalidateAllTokens(user.getId());
    }

    private record ManagerAndUser(User manager, User user) {}

    private ManagerAndUser validateManagerAndUser(Long managerId, Long userId) {
        if (managerId.equals(userId)) {
            throw new ServiceErrorException(UserExceptionEnum.CANNOT_MODIFY_SELF);
        }

        User manager = userRepository.findByIdAndDeletedAtIsNull(managerId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        if (user.getRole().getLevel() >= manager.getRole().getLevel()) {
            throw new ServiceErrorException(UserExceptionEnum.USER_MODIFICATION_FORBIDDEN);
        }

        return new ManagerAndUser(manager, user);
    }
}
