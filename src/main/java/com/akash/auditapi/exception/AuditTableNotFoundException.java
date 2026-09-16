package com.akash.auditapi.exception;

import com.akash.auditapi.enums.ApiOutcomeCode;
import org.springframework.http.HttpStatus;

/**
 * Signals that a discovered source table lacks its required audit-table pair.
 * The API maps it to the table-pair-not-found client outcome.
 */
public class AuditTableNotFoundException extends AuditApiException {
    public AuditTableNotFoundException(String message) {
        super(ApiOutcomeCode.TABLE_PAIR_NOT_FOUND, message, HttpStatus.NOT_FOUND);
    }
}
