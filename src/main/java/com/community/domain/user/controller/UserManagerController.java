package com.community.domain.user.controller;

import com.community.common.dto.BaseResponse;
import com.community.domain.user.dto.request.UpdateUserRoleRequest;
import com.community.domain.user.dto.response.UpdateUserRoleResponse;
import com.community.domain.user.service.UserManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/users")
public class UserManagerController {

    private final UserManagerService userManagerService;

    // 회원 등급 변경
    @PatchMapping("/role")
    public ResponseEntity<BaseResponse<UpdateUserRoleResponse>> updateRole(
            @RequestBody UpdateUserRoleRequest request
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, userManagerService.updateRole(request)));
    }
}
