package com.fail.app.domain.category.dto.response;

import com.fail.app.domain.category.entity.Category;

public record CategoryResponse(
        Long categoryId,
        String name,
        String slug,
        Integer displayOrder
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDisplayOrder()
        );
    }
}
