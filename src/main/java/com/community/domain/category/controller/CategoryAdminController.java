package com.community.domain.category.controller;

import com.community.common.dto.BaseResponse;
import com.community.domain.category.dto.request.CreateCategoryRequest;
import com.community.domain.category.dto.response.CreateCategoryResponse;
import com.community.domain.category.service.CategoryAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/categories")
public class CategoryAdminController {

    private final CategoryAdminService categoryAdminService;

    // 카테고리 등록
    @PostMapping
    public ResponseEntity<BaseResponse<CreateCategoryResponse>> create(
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                HttpStatus.CREATED.name(), null, categoryAdminService.create(request)));
    }
}
