package com.akash.auditapi.model;

import org.springframework.http.HttpStatus;

import java.time.Instant;

public final class ApiResponse<T> extends BaseApiResponse<ApiOutcomeCode> {
    private final T data;

    private ApiResponse(HttpStatus status, String message, T data) {
        super(Instant.now(), status.value(), ApiOutcomeCode.SUCCESS, message);
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(HttpStatus.OK, message, data);
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return new ApiResponse<>(HttpStatus.CREATED, message, data);
    }

    public T getData() {
        return data;
    }
}
