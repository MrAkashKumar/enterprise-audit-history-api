package com.akash.auditapi.exception;

import com.akash.auditapi.enums.ApiOutcomeCode;
import org.springframework.http.HttpStatus;

/**
 * Signals that a source or audit table is missing a required configured column.
 * Descriptor verification raises it before any dynamic row query runs.
 */
public class MissingAuditColumnException extends AuditApiException {
    public MissingAuditColumnException(String table, String column) {
        super(ApiOutcomeCode.MISSING_REQUIRED_COLUMN,
                ApiMessages.requiredColumnNotFound(table, column),
                HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
