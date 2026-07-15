package com.fail.app.common.exception;

import com.fail.app.common.response.ErrorResponse;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
        ErrorCode errorCode = exception.getErrorCode();

        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.builder()
                        .code(ErrorCode.INVALID_INPUT.getCode())
                        .message(ErrorCode.INVALID_INPUT.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.builder()
                        .code(ErrorCode.INVALID_INPUT.getCode())
                        .message(ErrorCode.INVALID_INPUT.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        String detail = exception.getMostSpecificCause().getMessage().toLowerCase();
        ErrorCode errorCode = resolveConstraintError(detail);
        if (errorCode == ErrorCode.INTERNAL_SERVER_ERROR) {
            log.error("Unhandled data integrity violation", exception);
        }
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    private ErrorCode resolveConstraintError(String detail) {
        if (detail.contains("uk_users_email") || detail.contains("users(email")) {
            return ErrorCode.EMAIL_ALREADY_EXISTS;
        }
        if (detail.contains("uk_users_nickname") || detail.contains("users(nickname")) {
            return ErrorCode.NICKNAME_ALREADY_EXISTS;
        }
        if (detail.contains("uk_report_reporter_target")) {
            return ErrorCode.DUPLICATE_REPORT;
        }
        if (detail.contains("uk_reaction_post_user")) {
            return ErrorCode.DUPLICATE_REACTION;
        }
        return ErrorCode.INTERNAL_SERVER_ERROR;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception) {
        log.error("Unhandled exception", exception);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.builder()
                        .code(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                        .message(ErrorCode.INTERNAL_SERVER_ERROR.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}
