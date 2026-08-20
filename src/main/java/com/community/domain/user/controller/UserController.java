package com.community.domain.user.controller;

import com.community.common.config.security.CustomUserDetails;
import com.community.common.dto.BaseResponse;
import com.community.domain.user.dto.request.UserUpdateNicknameRequest;
import com.community.domain.user.dto.response.UserGetMineResponse;
import com.community.domain.user.dto.response.UserGetOneResponse;
import com.community.domain.user.dto.response.UserUpdateNicknameResponse;
import com.community.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    // 마이페이지 조회
    @GetMapping("/me")
    public ResponseEntity<BaseResponse<UserGetMineResponse>> getMine(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "마이페이지를 조회하였습니다", userService.getMine(userId)));
    }

    // 닉네임 변경
    @PatchMapping("/nickname")
    public ResponseEntity<BaseResponse<UserUpdateNicknameResponse>> updateNickname(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserUpdateNicknameRequest request
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "닉네임을 변경하였습니다", userService.updateNickname(userId, request)));
    }
}
