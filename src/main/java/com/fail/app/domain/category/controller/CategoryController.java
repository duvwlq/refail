package com.fail.app.domain.category.controller;

import com.fail.app.domain.category.dto.response.CategoryResponse;
import com.fail.app.domain.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@Tag(name = "카테고리", description = "게시글 카테고리 조회")
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "활성 카테고리 목록 조회")
    public List<CategoryResponse> getCategories(@RequestParam(required = false) String keyword) {
        return categoryService.getCategories(keyword);
    }
}
