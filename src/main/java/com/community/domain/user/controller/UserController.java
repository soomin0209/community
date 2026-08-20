package com.community.domain.user.controller;

import com.community.common.dto.BaseResponse;
import com.community.domain.user.dto.response.UserGetOneResponse;
import com.community.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    // 프로필 조회
    @GetMapping("/{userId}")
    public ResponseEntity<BaseResponse<UserGetOneResponse>> getOne(@PathVariable Long userId) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "프로필을 조회하였습니다", userService.getOne(userId)));
    }
}
