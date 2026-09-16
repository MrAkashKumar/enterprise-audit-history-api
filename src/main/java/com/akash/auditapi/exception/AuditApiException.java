package com.akash.auditapi.exception;

import com.akash.auditapi.enums.ApiOutcomeCode;
import org.springframework.http.HttpStatus;

/**
 * Base type for expected API failures with an application code and HTTP status.
 * The global handler converts subclasses into the standard error envelope.
 */
public abstract class AuditApiException extends RuntimeException {
    private final ApiOutcomeCode code;
    private final HttpStatus status;

    protected AuditApiException(ApiOutcomeCode code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public ApiOutcomeCode getCode() { return code; }
    public HttpStatus getStatus() { return status; }
}
