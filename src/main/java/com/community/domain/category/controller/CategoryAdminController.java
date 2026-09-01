package com.community.domain.category.controller;

import com.community.domain.category.service.CategoryAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/categories")
public class CategoryAdminController {

    private final CategoryAdminService categoryAdminService;
}
