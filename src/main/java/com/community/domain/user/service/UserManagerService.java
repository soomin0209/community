package com.community.domain.user.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.user.dto.request.UpdateUserRoleRequest;
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

    // 회원 등급 변경
    public UpdateUserRoleResponse updateRole(UpdateUserRoleRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(request.userId()).orElseThrow(
                () -> new ServiceErrorException(UserExceptionEnum.USER_NOT_FOUND));

        if (user.getRole().getLevel() >= UserRole.MANAGER.getLevel() ||
                request.role().getLevel() >= UserRole.MANAGER.getLevel()) {
            throw new ServiceErrorException(UserExceptionEnum.UPDATE_ROLE_FORBIDDEN);
        }

        user.updateRoleByManager(request.role());

        return new UpdateUserRoleResponse(
                user.getId(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
