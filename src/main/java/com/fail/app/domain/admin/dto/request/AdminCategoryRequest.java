package com.fail.app.domain.admin.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCategoryRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Size(max = 50) String slug,
        @Min(0) int displayOrder
) {
}
