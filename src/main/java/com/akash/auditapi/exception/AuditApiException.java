package com.akash.auditapi.exception;

import org.springframework.http.HttpStatus;

public abstract class AuditApiException extends RuntimeException {
    private final ApiErrorCode code;
    private final HttpStatus status;

    protected AuditApiException(ApiErrorCode code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public ApiErrorCode getCode() { return code; }
    public HttpStatus getStatus() { return status; }
}
