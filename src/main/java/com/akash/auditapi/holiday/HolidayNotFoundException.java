package com.akash.auditapi.holiday;

import com.akash.auditapi.exception.ApiErrorCode;
import com.akash.auditapi.exception.AuditApiException;
import org.springframework.http.HttpStatus;

import static com.akash.auditapi.exception.ApiMessages.holidayNotFound;

public class HolidayNotFoundException extends AuditApiException {
    public HolidayNotFoundException(Long id) {
        super(ApiErrorCode.HOLIDAY_NOT_FOUND, holidayNotFound(id), HttpStatus.NOT_FOUND);
    }
}
