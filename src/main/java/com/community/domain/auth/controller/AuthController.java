package com.community.domain.auth.controller;

import com.community.common.annotation.Idempotent;
import com.community.common.config.security.CustomUserDetails;
import com.community.common.dto.BaseResponse;
import com.community.common.exception.ServiceErrorException;
import com.community.domain.auth.dto.request.AuthLoginRequest;
import com.community.domain.auth.dto.request.AuthSignupRequest;
import com.community.domain.auth.dto.response.AuthLoginResponse;
import com.community.domain.auth.dto.response.AuthSignupResponse;
import com.community.domain.auth.exception.AuthExceptionEnum;
import com.community.domain.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    // 회원가입
    @Idempotent
    @PostMapping("/signup")
    public ResponseEntity<BaseResponse<AuthSignupResponse>> signup(@Valid @RequestBody AuthSignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                HttpStatus.CREATED.name(), "회원가입했습니다", authService.signup(request)));
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<AuthLoginResponse>> login(
            @Valid @RequestBody AuthLoginRequest request,
            HttpServletResponse response
    ) {
        AuthLoginResponse loginResponse = authService.login(request);

        Cookie cookie = new Cookie("refreshToken", loginResponse.refreshToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(604800);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);

        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "로그인했습니다", new AuthLoginResponse(loginResponse.accessToken(), null)));
    }

    // 토큰 재발급
    @PostMapping("/reissue")
    public ResponseEntity<BaseResponse<AuthLoginResponse>> reissue(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ServiceErrorException(AuthExceptionEnum.REFRESH_TOKEN_NOT_FOUND);
        }

        AuthLoginResponse reissueResponse = authService.reissue(refreshToken);

        Cookie newCookie = new Cookie("refreshToken", reissueResponse.refreshToken());
        newCookie.setHttpOnly(true);
        newCookie.setSecure(cookieSecure);
        newCookie.setPath("/");
        newCookie.setMaxAge(604800);
        newCookie.setAttribute("SameSite", "Strict");
        response.addCookie(newCookie);

        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "토큰을 갱신했습니다", new AuthLoginResponse(reissueResponse.accessToken(), null)));
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Long userId = userDetails.getUserId();
        String accessToken = (String) request.getAttribute("accessToken");
        authService.logout(userId, accessToken);

        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);    // 즉시 만료
        response.addCookie(cookie);

        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), "로그아웃했습니다", null));
    }
}
