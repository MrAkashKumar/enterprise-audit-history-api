package com.akash.auditapi.holiday;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record HolidayRequest(
        @NotNull @Positive Long id,
        @NotNull LocalDate holidayDate,
        @NotBlank @Size(max = 50) String calendarCode,
        @NotBlank @Size(max = 200) String calendarName,
        @NotBlank @Size(max = 128) String username
) {}
