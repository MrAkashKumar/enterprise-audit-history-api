package com.akash.auditapi.dto.response;

import com.akash.auditapi.enums.ApiOutcomeCode;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.Objects;

/**
 * Stores metadata shared by every success and error response.
 * Concrete envelopes add either endpoint data or error details.
 */
public abstract class BaseApiResponse {
    private final Instant timestamp;
    private final ApiOutcomeCode status;
    private final String code;
    private final String message;

    protected BaseApiResponse(Instant timestamp, ApiOutcomeCode code, String message) {
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        this.status = Objects.requireNonNull(code, "status must not be null");
        this.code = code.applicationCode();
        this.message = Objects.requireNonNull(message, "message must not be null");
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public Instant getTimestamp() {
        return timestamp;
    }

    public ApiOutcomeCode getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
