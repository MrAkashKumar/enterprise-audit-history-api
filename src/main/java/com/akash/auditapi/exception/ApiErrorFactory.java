package com.akash.auditapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

import static com.akash.auditapi.trace.TraceContext.currentTraceId;

/** Creates the common error envelope used by MVC advice and servlet filters. */
@Component
public class ApiErrorFactory {
    public ApiError create(HttpStatus status, ApiErrorCode code, String message) {
        return create(status, code, message, List.of());
    }

    public ApiError create(HttpStatus status, ApiErrorCode code, String message,
                           List<ApiFieldError> details) {
        return new ApiError(Instant.now(), currentTraceId(), status.value(),
                status.getReasonPhrase(), code, message, details);
    }
}
