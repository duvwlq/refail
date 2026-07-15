package com.fail.app.domain.post.entity;

import com.fail.app.common.exception.ApiException;
import com.fail.app.common.exception.ErrorCode;
import java.util.Locale;

public enum PostSortType {
    LATEST,
    POPULAR;

    public static PostSortType from(String value) {
        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ApiException(ErrorCode.INVALID_INPUT);
        }
    }
}
