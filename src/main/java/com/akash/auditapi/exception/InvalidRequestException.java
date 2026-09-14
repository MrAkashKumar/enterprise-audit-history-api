package com.akash.auditapi.exception;

import com.akash.auditapi.model.ApiOutcomeCode;
import org.springframework.http.HttpStatus;

public class InvalidRequestException extends AuditApiException {
    public InvalidRequestException(ApiOutcomeCode code, String message) {
        super(code, message, HttpStatus.BAD_REQUEST);
    }
}
