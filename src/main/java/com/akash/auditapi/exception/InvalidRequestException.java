package com.akash.auditapi.exception;

import com.akash.auditapi.enums.ApiOutcomeCode;
import org.springframework.http.HttpStatus;

/**
 * Represents an invalid client input with a specific application outcome code.
 * Validators and resolvers use it for safe HTTP 400 responses.
 */
public class InvalidRequestException extends AuditApiException {
    public InvalidRequestException(ApiOutcomeCode code, String message) {
        super(code, message, HttpStatus.BAD_REQUEST);
    }
}
