package com.community.domain.category.service;

import com.community.domain.category.dto.response.GetAllCategoriesResponse;
import com.community.domain.category.entity.Category;
import com.community.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // 카테고리 목록 조회
    public List<GetAllCategoriesResponse> getAll() {
        List<Category> categoryList = categoryRepository.findAll();

        return categoryList.stream()
                .map(category -> new GetAllCategoriesResponse(
                        category.getId(),
                        category.getName()))
                .toList();
    }
}
