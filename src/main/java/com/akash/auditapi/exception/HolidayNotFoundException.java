package com.akash.auditapi.exception;

import com.akash.auditapi.enums.ApiOutcomeCode;
import org.springframework.http.HttpStatus;

import static com.akash.auditapi.exception.ApiMessages.holidayNotFound;

public class HolidayNotFoundException extends AuditApiException {
    public HolidayNotFoundException(Long id) {
        super(ApiOutcomeCode.HOLIDAY_NOT_FOUND, holidayNotFound(id), HttpStatus.NOT_FOUND);
    }
}
