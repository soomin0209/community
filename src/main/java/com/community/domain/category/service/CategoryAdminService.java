package com.community.domain.category.service;

import com.community.common.exception.ServiceErrorException;
import com.community.domain.category.dto.request.CreateCategoryRequest;
import com.community.domain.category.dto.request.UpdateCategoryRequest;
import com.community.domain.category.dto.response.CreateCategoryResponse;
import com.community.domain.category.dto.response.UpdateCategoryResponse;
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

    // 카테고리 수정
    public UpdateCategoryResponse update(Long categoryId, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(
                () -> new ServiceErrorException(CategoryExceptionEnum.CATEGORY_NOT_FOUND));

        if (request.name().equals(category.getName())) {
            throw new ServiceErrorException(CategoryExceptionEnum.NAME_UNCHANGED);
        }

        if (categoryRepository.existsByName(request.name())) {
            throw new ServiceErrorException(CategoryExceptionEnum.DUPLICATED_NAME);
        }

        category.update(request.name());

        return new UpdateCategoryResponse(
                category.getId(),
                category.getName(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
