package com.akash.auditapi.exception;

import com.akash.auditapi.enums.ApiOutcomeCode;
import org.springframework.http.HttpStatus;

public class AuditTableNotFoundException extends AuditApiException {
    public AuditTableNotFoundException(String message) {
        super(ApiOutcomeCode.TABLE_PAIR_NOT_FOUND, message, HttpStatus.NOT_FOUND);
    }
}
