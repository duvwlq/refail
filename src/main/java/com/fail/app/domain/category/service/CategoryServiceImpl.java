package com.fail.app.domain.category.service;

import com.fail.app.domain.category.dto.response.CategoryResponse;
import com.fail.app.domain.category.repository.CategoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Cacheable(
            cacheNames = "activeCategories",
            key = "'all'",
            condition = "#keyword == null || #keyword.isBlank()"
    )
    public List<CategoryResponse> getCategories(String keyword) {
        var categories = keyword == null || keyword.isBlank()
                ? categoryRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc()
                : categoryRepository.searchActive(keyword.trim());
        return categories
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }
}
