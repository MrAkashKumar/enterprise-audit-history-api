package com.akash.auditapi.dto.response;

import com.akash.auditapi.enums.ApiOutcomeCode;

import java.time.Instant;

/**
 * Wraps successful endpoint data in the shared response metadata contract.
 * Controllers create it through the {@link #success(Object, String)} factory.
 */
public final class ApiResponse<T> extends BaseApiResponse {
    private final T data;

    private ApiResponse(String message, T data) {
        super(Instant.now(), ApiOutcomeCode.SUCCESS, message);
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(message, data);
    }

    public T getData() {
        return data;
    }
}
