package com.akash.auditapi.exception;

import org.springframework.http.HttpStatus;

public class TableNotAllowedException extends AuditApiException {
    public TableNotAllowedException(String table) {
        super(ApiErrorCode.TABLE_NOT_ALLOWED, ApiMessages.tableNotExposed(table),
                HttpStatus.NOT_FOUND);
    }
}
