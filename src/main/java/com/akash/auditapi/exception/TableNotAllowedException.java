package com.akash.auditapi.exception;

import com.akash.auditapi.enums.ApiOutcomeCode;
import org.springframework.http.HttpStatus;

/**
 * Signals that a requested table is absent from the dynamic source-table catalog.
 * It prevents unlisted identifiers from reaching dynamic SQL construction.
 */
public class TableNotAllowedException extends AuditApiException {
    public TableNotAllowedException(String table) {
        super(ApiOutcomeCode.TABLE_NOT_ALLOWED, ApiMessages.tableNotExposed(table),
                HttpStatus.NOT_FOUND);
    }
}
