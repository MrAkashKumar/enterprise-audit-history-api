package com.akash.auditapi.exceptionHandlers;

import com.akash.auditapi.enums.ApiOutcomeCode;
import com.akash.auditapi.exception.ApiError;
import com.akash.auditapi.exception.ApiFieldError;
import com.akash.auditapi.exception.AuditApiException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

import static com.akash.auditapi.exception.ApiMessages.DATABASE_FAILURE_LOG;
import static com.akash.auditapi.exception.ApiMessages.DATABASE_QUERY_FAILED;
import static com.akash.auditapi.exception.ApiMessages.INVALID_REQUEST;
import static com.akash.auditapi.exception.ApiMessages.INVALID_VALUE;
import static com.akash.auditapi.exception.ApiMessages.UNEXPECTED_ERROR;
import static com.akash.auditapi.exception.ApiMessages.UNEXPECTED_FAILURE_LOG;
import static com.akash.auditapi.exception.ApiMessages.VALIDATION_FAILED;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(AuditApiException.class)
    ResponseEntity<ApiError> auditApiError(AuditApiException exception) {
        return error(exception.getStatus(), exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> bodyValidationError(MethodArgumentNotValidException exception) {
        List<ApiFieldError> details = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ApiFieldError(fieldError.getField(),
                        fieldError.getDefaultMessage() == null
                                ? INVALID_VALUE : fieldError.getDefaultMessage()))
                .toList();
        return error(HttpStatus.BAD_REQUEST, ApiOutcomeCode.VALIDATION_FAILED,
                VALIDATION_FAILED, details);
    }

    @ExceptionHandler({HandlerMethodValidationException.class,
            ConstraintViolationException.class, HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class, ArithmeticException.class})
    ResponseEntity<ApiError> invalidRequest() {
        return error(HttpStatus.BAD_REQUEST, ApiOutcomeCode.INVALID_REQUEST,
                INVALID_REQUEST);
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ApiError> databaseError(DataAccessException exception) {
        log.error(DATABASE_FAILURE_LOG, exception.getClass().getSimpleName());
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ApiOutcomeCode.DATABASE_ERROR,
                DATABASE_QUERY_FAILED);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpectedError(Exception exception) {
        log.error(UNEXPECTED_FAILURE_LOG, ApiOutcomeCode.INTERNAL_ERROR.applicationCode(),
                exception.getClass().getSimpleName());
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ApiOutcomeCode.INTERNAL_ERROR,
                UNEXPECTED_ERROR);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, ApiOutcomeCode code, String message) {
        return error(status, code, message, List.of());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, ApiOutcomeCode code, String message,
                                           List<ApiFieldError> details) {
        ApiError body = new ApiError(Instant.now(), status.getReasonPhrase(), code, message, details);
        return ResponseEntity.status(status).body(body);
    }
}
