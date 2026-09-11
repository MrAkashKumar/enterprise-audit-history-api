package com.akash.auditapi.exception;

import org.springframework.http.HttpStatus;

public class AuditTableNotFoundException extends AuditApiException {
    public AuditTableNotFoundException(String message) {
        super(ApiErrorCode.TABLE_PAIR_NOT_FOUND, message, HttpStatus.NOT_FOUND);
    }
}
