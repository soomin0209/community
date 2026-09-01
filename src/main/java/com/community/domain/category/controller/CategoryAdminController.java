package com.community.domain.category.controller;

import com.community.common.dto.BaseResponse;
import com.community.domain.category.dto.request.CreateCategoryRequest;
import com.community.domain.category.dto.request.UpdateCategoryRequest;
import com.community.domain.category.dto.response.CreateCategoryResponse;
import com.community.domain.category.dto.response.UpdateCategoryResponse;
import com.community.domain.category.service.CategoryAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // 카테고리 수정
    @PatchMapping("/{categoryId}")
    public ResponseEntity<BaseResponse<UpdateCategoryResponse>> update(
            @PathVariable Long categoryId,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, categoryAdminService.update(categoryId, request)));
    }

    // 카테고리 삭제
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable Long categoryId) {
        categoryAdminService.delete(categoryId);
        return ResponseEntity.status(HttpStatus.OK).body(BaseResponse.success(
                HttpStatus.OK.name(), null, null));
    }
}
