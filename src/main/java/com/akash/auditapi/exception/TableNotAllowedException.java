package com.akash.auditapi.exception;

import com.akash.auditapi.enums.ApiOutcomeCode;
import org.springframework.http.HttpStatus;

public class TableNotAllowedException extends AuditApiException {
    public TableNotAllowedException(String table) {
        super(ApiOutcomeCode.TABLE_NOT_ALLOWED, ApiMessages.tableNotExposed(table),
                HttpStatus.NOT_FOUND);
    }
}
