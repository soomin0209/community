package com.community.domain.user.controller;

import com.community.common.config.security.CustomUserDetails;
import com.community.common.dto.BaseResponse;
import com.community.domain.auth.service.AuthService;
import com.community.domain.user.dto.request.UserUpdateNicknameRequest;
import com.community.domain.user.dto.request.UserUpdatePasswordRequest;
import com.community.domain.user.dto.response.*;
import com.community.domain.user.enums.UserRankType;
import com.community.domain.user.service.UserRankingService;
import com.community.domain.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final UserRankingService userRankingService;

    // 프로필 조회
    @GetMapping("/{userId}")
    public ResponseEntity<BaseResponse<UserGetOneResponse>> getOne(@PathVariable Long userId) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, userService.getOne(userId)));
    }

    // 마이페이지 조회
    @GetMapping("/me")
    public ResponseEntity<BaseResponse<UserGetMineResponse>> getMine(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, userService.getMine(userId)));
    }

    // 주간 사용자 랭킹 조회 (Top 5)
    @GetMapping("/ranking")
    public ResponseEntity<BaseResponse<List<UserGetRanking5Response>>> getRanking(
            @RequestParam(defaultValue = "COMMENT") UserRankType type
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, userRankingService.getWeeklyUserRanking(type)));
    }

    // 닉네임 변경
    @PatchMapping("/nickname")
    public ResponseEntity<BaseResponse<UserUpdateNicknameResponse>> updateNickname(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserUpdateNicknameRequest request
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, userService.updateNickname(userId, request)));
    }

    // 비밀번호 변경
    @PatchMapping("/password")
    public ResponseEntity<BaseResponse<UserUpdatePasswordResponse>> updatePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserUpdatePasswordRequest request
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, userService.updatePassword(userId, request)));
    }

    // 회원 탈퇴
    @DeleteMapping("/me")
    public ResponseEntity<BaseResponse<Void>> withdraw(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Long userId = userDetails.getUserId();
        String accessToken = (String) request.getAttribute("accessToken");
        userService.withdraw(userId);
        authService.logout(userId, accessToken);

        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);    // 즉시 만료
        response.addCookie(cookie);

        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, null));
    }
}
