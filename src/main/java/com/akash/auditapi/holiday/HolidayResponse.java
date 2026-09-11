package com.akash.auditapi.holiday;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record HolidayResponse(Long id, LocalDate holidayDate, String calendarCode,
                              String calendarName, Long version, String createdBy,
                              LocalDateTime createdOn, String updatedBy, LocalDateTime updatedOn) {
    public static HolidayResponse from(Holiday holiday) {
        return new HolidayResponse(holiday.getId(), holiday.getHolidayDate(), holiday.getCalendarCode(),
                holiday.getCalendarName(), holiday.getVersion(), holiday.getCreatedBy(),
                holiday.getCreatedOn(), holiday.getUpdatedBy(), holiday.getUpdatedOn());
    }
}
