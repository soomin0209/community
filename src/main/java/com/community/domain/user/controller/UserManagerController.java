package com.community.domain.user.controller;

import com.community.common.config.security.CustomUserDetails;
import com.community.common.dto.BaseResponse;
import com.community.domain.user.dto.request.SuspendUserRequest;
import com.community.domain.user.dto.request.UpdateUserRoleRequest;
import com.community.domain.user.dto.response.SuspendUserResponse;
import com.community.domain.user.dto.response.UpdateUserRoleResponse;
import com.community.domain.user.service.UserManagerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/users")
public class UserManagerController {

    private final UserManagerService userManagerService;

    // 회원 등급 변경
    @PatchMapping("/{userId}/role")
    public ResponseEntity<BaseResponse<UpdateUserRoleResponse>> updateRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, userManagerService.updateRole(userId, request)));
    }

    // 회원 활동 정지
    @PatchMapping("/{userId}/suspend")
    public ResponseEntity<BaseResponse<SuspendUserResponse>> suspend(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long userId,
            @Valid @RequestBody SuspendUserRequest request
    ) {
        Long managerId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, userManagerService.suspend(managerId, userId, request)));
    }

    // 회원 강제 탈퇴
    @DeleteMapping("/{userId}")
    public ResponseEntity<BaseResponse<Void>> withdraw(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long userId
    ) {
        Long managerId = userDetails.getUserId();
        userManagerService.withdraw(managerId, userId);
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, null));
    }
}
