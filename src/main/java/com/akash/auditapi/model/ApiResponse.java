package com.akash.auditapi.model;

import java.time.Instant;

public final class ApiResponse<T> extends BaseApiResponse {
    private final T data;

    private ApiResponse(String message, T data) {
        super(Instant.now(), ApiOutcomeCode.SUCCESS, message);
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(message, data);
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return new ApiResponse<>(message, data);
    }

    public T getData() {
        return data;
    }
}
