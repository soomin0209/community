package com.community.domain.category.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.category.dto.request.CreateCategoryRequest;
import com.community.domain.category.dto.response.CreateCategoryResponse;
import com.community.domain.category.entity.Category;
import com.community.domain.category.exception.CategoryExceptionEnum;
import com.community.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryAdminService {

    private final CategoryRepository categoryRepository;

    // 카테고리 등록
    public CreateCategoryResponse create(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new ServiceErrorException(CategoryExceptionEnum.DUPLICATED_NAME);
        }

        Category category = Category.register(request.name());
        categoryRepository.save(category);

        return new CreateCategoryResponse(category.getId(), category.getName(), category.getCreatedAt());
    }
}
