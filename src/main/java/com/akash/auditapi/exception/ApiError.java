package com.akash.auditapi.exception;

import com.akash.auditapi.model.BaseApiResponse;

import java.time.Instant;
import java.util.List;

public final class ApiError extends BaseApiResponse<ApiErrorCode> {
    private final String traceId;
    private final String error;
    private final List<ApiFieldError> details;

    public ApiError(Instant timestamp, String traceId, int status, String error, ApiErrorCode code,
                    String message, List<ApiFieldError> details) {
        super(timestamp, status, code, message);
        this.traceId = traceId;
        this.error = error;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public String getTraceId() { return traceId; }
    public String getError() { return error; }
    public List<ApiFieldError> getDetails() { return details; }
    public Object getData() { return null; }
}
