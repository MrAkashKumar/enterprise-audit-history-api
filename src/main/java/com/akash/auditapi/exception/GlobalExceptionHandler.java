package com.akash.auditapi.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.List;

import static com.akash.auditapi.model.ApiOutcomeCode.UNKNOWN_ERROR;

import static com.akash.auditapi.exception.ApiMessages.DATABASE_FAILURE_LOG;
import static com.akash.auditapi.exception.ApiMessages.DATABASE_QUERY_FAILED;
import static com.akash.auditapi.exception.ApiMessages.INVALID_REQUEST;
import static com.akash.auditapi.exception.ApiMessages.INVALID_VALUE;
import static com.akash.auditapi.exception.ApiMessages.UNEXPECTED_ERROR;
import static com.akash.auditapi.exception.ApiMessages.UNEXPECTED_FAILURE_LOG;
import static com.akash.auditapi.exception.ApiMessages.VALIDATION_FAILED;
import static com.akash.auditapi.trace.TraceContext.HEADER_NAME;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ApiErrorFactory errorFactory;

    public GlobalExceptionHandler(ApiErrorFactory errorFactory) {
        this.errorFactory = errorFactory;
    }

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
        return error(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED,
                VALIDATION_FAILED, details);
    }

    @ExceptionHandler({HandlerMethodValidationException.class,
            ConstraintViolationException.class, HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class, ArithmeticException.class})
    ResponseEntity<ApiError> invalidRequest() {
        return error(HttpStatus.BAD_REQUEST, ApiErrorCode.INVALID_REQUEST,
                INVALID_REQUEST);
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ApiError> databaseError(DataAccessException exception, HttpServletRequest request) {
        LOGGER.error(DATABASE_FAILURE_LOG, request.getRequestURI(), exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.DATABASE_ERROR,
                DATABASE_QUERY_FAILED);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpectedError(Exception exception, HttpServletRequest request) {
        LOGGER.error(UNEXPECTED_FAILURE_LOG, UNKNOWN_ERROR.internalCode(),
                request.getRequestURI(), exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ApiErrorCode.INTERNAL_ERROR,
                UNEXPECTED_ERROR);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, ApiErrorCode code, String message) {
        return error(status, code, message, List.of());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, ApiErrorCode code, String message,
                                           List<ApiFieldError> details) {
        ApiError body = errorFactory.create(status, code, message, details);
        ResponseEntity.BodyBuilder response = ResponseEntity.status(status);
        if (body.getTraceId() != null) {
            response.header(HEADER_NAME, body.getTraceId());
        }
        return response.body(body);
    }
}
