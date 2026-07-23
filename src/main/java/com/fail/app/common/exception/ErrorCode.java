package com.fail.app.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_002", "요청한 리소스를 찾을 수 없습니다."),
    USER_DELETED(HttpStatus.FORBIDDEN, "USER_005", "삭제된 사용자입니다."),
    CATEGORY_INACTIVE(HttpStatus.CONFLICT, "CATEGORY_002", "비활성화된 카테고리는 선택할 수 없습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_001", "잘못된 입력입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_001", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_002", "권한이 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),
    USER_RESTRICTED(HttpStatus.FORBIDDEN, "USER_002", "제한된 사용자입니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATEGORY_001", "카테고리를 찾을 수 없습니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_001", "게시글을 찾을 수 없습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_003", "이메일 또는 비밀번호가 올바르지 않습니다."),
    REFRESH_TOKEN_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH_004", "리프레시 토큰이 필요합니다."),
    REFRESH_TOKEN_ALREADY_ROTATED(HttpStatus.UNAUTHORIZED, "AUTH_005", "이미 갱신된 리프레시 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_006", "유효하지 않은 리프레시 토큰입니다."),
    REFRESH_TOKEN_REUSED(HttpStatus.UNAUTHORIZED, "AUTH_007", "리프레시 토큰 재사용이 감지되어 세션을 종료했습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_003", "이미 사용 중인 이메일입니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_004", "이미 사용 중인 닉네임입니다."),
    DUPLICATE_REPORT(HttpStatus.CONFLICT, "REPORT_001", "이미 신고한 게시글입니다."),
    DUPLICATE_REACTION(HttpStatus.CONFLICT, "REACTION_001", "이미 처리된 공감 요청입니다."),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "RATE_001", "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_999", "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
