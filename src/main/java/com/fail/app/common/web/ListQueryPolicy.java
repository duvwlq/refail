package com.fail.app.common.web;

import com.fail.app.common.exception.ApiException;
import com.fail.app.common.exception.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class ListQueryPolicy {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 50;
    public static final int POST_KEYWORD_MAX_LENGTH = 100;
    public static final int CATEGORY_KEYWORD_MAX_LENGTH = 50;

    private ListQueryPolicy() {
    }

    public static PageRequest pageRequest(int page, int size) {
        validatePage(page, size);
        return PageRequest.of(page, size);
    }

    public static PageRequest pageRequest(int page, int size, Sort sort) {
        validatePage(page, size);
        return PageRequest.of(page, size, sort);
    }

    public static String normalizeKeyword(String keyword, int maxLength) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String normalized = keyword.trim();
        if (normalized.length() > maxLength) {
            throw new ApiException(ErrorCode.INVALID_INPUT);
        }
        return normalized;
    }

    private static void validatePage(int page, int size) {
        if (page < DEFAULT_PAGE || size < 1 || size > MAX_SIZE) {
            throw new ApiException(ErrorCode.INVALID_INPUT);
        }
    }
}
