package com.akash.auditapi.dao;

import com.akash.auditapi.entity.Holiday;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class HolidayRepositoryTest {
    @Autowired
    private HolidayRepository repository;

    @Test
    void persistsAndFindsHolidayByAssignedId() {
        Holiday holiday = new Holiday(
                900001L, LocalDate.of(2030, 1, 1), "TST", "Test Calendar", "tester");

        repository.saveAndFlush(holiday);

        assertThat(repository.findById(900001L)).contains(holiday);
    }
}
