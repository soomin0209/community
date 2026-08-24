package com.community.domain.auth.controller;

import com.community.common.annotation.Idempotent;
import com.community.common.dto.BaseResponse;
import com.community.domain.auth.dto.request.AuthAdminSignupRequest;
import com.community.domain.auth.dto.response.AuthSignupResponse;
import com.community.domain.auth.service.AuthAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/auth")
public class AuthAdminController {

    private final AuthAdminService authAdminService;

    // 관리자 회원가입
    @Idempotent
    @PostMapping("/signup")
    public ResponseEntity<BaseResponse<AuthSignupResponse>> signup(@Valid @RequestBody AuthAdminSignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                HttpStatus.CREATED.name(), null, authAdminService.signup(request)));
    }
}
