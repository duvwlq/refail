package com.fail.app.domain.category.service;

import com.fail.app.domain.category.dto.response.CategoryResponse;
import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getCategories(String keyword);
}
