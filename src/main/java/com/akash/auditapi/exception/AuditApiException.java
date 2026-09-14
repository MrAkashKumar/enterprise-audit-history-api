package com.akash.auditapi.exception;

import com.akash.auditapi.model.ApiOutcomeCode;
import org.springframework.http.HttpStatus;

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
