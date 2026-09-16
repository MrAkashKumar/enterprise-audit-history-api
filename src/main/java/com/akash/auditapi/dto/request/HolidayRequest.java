package com.akash.auditapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Defines validated input for creating or updating a Holiday record.
 * Controllers accept this DTO instead of binding directly to the JPA entity.
 */
public record HolidayRequest(
        @NotNull @Positive Long id,
        @NotNull LocalDate holidayDate,
        @NotBlank @Size(max = 50) String calendarCode,
        @NotBlank @Size(max = 200) String calendarName,
        @NotBlank @Size(max = 128) String username
) {}
