package com.akash.auditapi.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class HolidayTest {
    @Test
    void maintainsLifecycleFieldsAndMutableBusinessValues() {
        Holiday holiday = new Holiday(1L, LocalDate.of(2030, 1, 1), "TST", "Initial", "creator");
        holiday.created();
        holiday.update(LocalDate.of(2030, 1, 2), "SIX", "Updated", "editor");
        holiday.updated();

        assertThat(holiday.getCreatedOn()).isNotNull();
        assertThat(holiday.getUpdatedOn()).isNotNull();
        assertThat(holiday.getUpdatedBy()).isEqualTo("editor");
        assertThat(holiday.getCalendarName()).isEqualTo("Updated");
    }
}
