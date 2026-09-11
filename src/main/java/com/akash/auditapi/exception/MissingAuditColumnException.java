package com.akash.auditapi.exception;

import org.springframework.http.HttpStatus;

public class MissingAuditColumnException extends AuditApiException {
    public MissingAuditColumnException(String table, String column) {
        super(ApiErrorCode.MISSING_REQUIRED_COLUMN,
                ApiMessages.requiredColumnNotFound(table, column),
                HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
