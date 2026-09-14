package com.akash.auditapi.validation;

import com.akash.auditapi.config.AuditApiProperties;
import com.akash.auditapi.enums.ApiOutcomeCode;
import com.akash.auditapi.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import static com.akash.auditapi.exception.ApiMessages.INVALID_PAGE_NUMBER;
import static com.akash.auditapi.exception.ApiMessages.invalidPageSize;

@Component
public class PaginationValidator {
    private final AuditApiProperties properties;

    public PaginationValidator(AuditApiProperties properties) {
        this.properties = properties;
    }

    public void validate(int pageNo, int pageSize) {
        if (pageNo < 0) {
            throw new InvalidRequestException(ApiOutcomeCode.INVALID_PAGE_NO,
                    INVALID_PAGE_NUMBER);
        }
        if (pageSize < 1 || pageSize > properties.maxPageSize()) {
            throw new InvalidRequestException(ApiOutcomeCode.INVALID_PAGE_SIZE,
                    invalidPageSize(properties.maxPageSize()));
        }
    }
}
