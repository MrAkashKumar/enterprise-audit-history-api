package com.akash.auditapi.exception;

import com.akash.auditapi.model.ApiOutcomeCode;
import org.springframework.http.HttpStatus;

public class MissingAuditColumnException extends AuditApiException {
    public MissingAuditColumnException(String table, String column) {
        super(ApiOutcomeCode.MISSING_REQUIRED_COLUMN,
                ApiMessages.requiredColumnNotFound(table, column),
                HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
