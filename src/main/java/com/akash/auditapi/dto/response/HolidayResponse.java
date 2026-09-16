package com.akash.auditapi.dto.response;

import com.akash.auditapi.entity.Holiday;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Exposes the approved Holiday fields returned by CRUD endpoints.
 * It keeps persistence entities outside the public API contract.
 */
public record HolidayResponse(Long id, LocalDate holidayDate, String calendarCode,
                              String calendarName, Long version, String createdBy,
                              LocalDateTime createdOn, String updatedBy, LocalDateTime updatedOn) {
    public static HolidayResponse from(Holiday holiday) {
        return new HolidayResponse(holiday.getId(), holiday.getHolidayDate(), holiday.getCalendarCode(),
                holiday.getCalendarName(), holiday.getVersion(), holiday.getCreatedBy(),
                holiday.getCreatedOn(), holiday.getUpdatedBy(), holiday.getUpdatedOn());
    }
}
