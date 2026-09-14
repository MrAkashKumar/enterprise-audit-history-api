package com.akash.auditapi.exception;

import com.akash.auditapi.dto.response.BaseApiResponse;
import com.akash.auditapi.enums.ApiOutcomeCode;

import java.time.Instant;
import java.util.List;

public final class ApiError extends BaseApiResponse {
    private final String error;
    private final List<ApiFieldError> details;

    public ApiError(Instant timestamp, String error, ApiOutcomeCode code,
                    String message, List<ApiFieldError> details) {
        super(timestamp, code, message);
        this.error = error;
        this.details = List.copyOf(details);
    }

    public String getError() { return error; }
    public List<ApiFieldError> getDetails() { return details; }
    public Object getData() { return null; }
}
