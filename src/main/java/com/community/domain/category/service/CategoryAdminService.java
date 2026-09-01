package com.community.domain.category.service;

import com.community.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryAdminService {

    private final CategoryRepository categoryRepository;
}
