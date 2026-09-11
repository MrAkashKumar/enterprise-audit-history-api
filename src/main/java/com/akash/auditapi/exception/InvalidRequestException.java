package com.akash.auditapi.exception;

import org.springframework.http.HttpStatus;

public class InvalidRequestException extends AuditApiException {
    public InvalidRequestException(ApiErrorCode code, String message) {
        super(code, message, HttpStatus.BAD_REQUEST);
    }
}
