package com.fail.app.domain.admin.service;

import com.fail.app.common.exception.ApiException;
import com.fail.app.common.exception.ErrorCode;
import com.fail.app.domain.admin.dto.request.AdminCategoryRequest;
import com.fail.app.domain.category.dto.response.CategoryResponse;
import com.fail.app.domain.category.entity.Category;
import com.fail.app.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminCategoryManager {

    private final CategoryRepository categoryRepository;

    @Transactional
    @CacheEvict(cacheNames = "activeCategories", allEntries = true)
    public CategoryResponse create(AdminCategoryRequest request) {
        Category category = Category.builder()
                .name(request.name())
                .slug(request.slug())
                .displayOrder(request.displayOrder())
                .isActive(true)
                .build();
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    @CacheEvict(cacheNames = "activeCategories", allEntries = true)
    public CategoryResponse update(Long categoryId, AdminCategoryRequest request) {
        Category category = getCategory(categoryId);
        category.update(request.name(), request.slug(), request.displayOrder());
        return CategoryResponse.from(category);
    }

    @Transactional
    @CacheEvict(cacheNames = "activeCategories", allEntries = true)
    public void deactivate(Long categoryId) {
        getCategory(categoryId).deactivate();
    }

    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND));
    }
}
