package com.akash.auditapi.service;

import com.akash.auditapi.dao.HolidayRepository;
import com.akash.auditapi.dto.request.HolidayRequest;
import com.akash.auditapi.entity.Holiday;
import com.akash.auditapi.exception.HolidayNotFoundException;
import com.akash.auditapi.validation.PaginationValidator;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HolidayServiceTest {
    private final HolidayRepository repository = mock(HolidayRepository.class);
    private final PaginationValidator paginationValidator = mock(PaginationValidator.class);
    private final HolidayService service = new HolidayService(repository, paginationValidator);
    private final HolidayRequest request = new HolidayRequest(
            900001L, LocalDate.of(2030, 1, 1), "TST", "Test Calendar", "tester");

    @Test
    void createsUpdatesDeletesAndPagesThroughJpaRepository() {
        Holiday holiday = new Holiday(900001L, request.holidayDate(), request.calendarCode(),
                request.calendarName(), request.username());
        when(repository.save(any(Holiday.class))).thenReturn(holiday);
        when(repository.findById(900001L)).thenReturn(Optional.of(holiday));
        when(repository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(holiday)));

        assertThat(service.create(request).getId()).isEqualTo(900001L);
        assertThat(service.findAll(0, 10).getContent()).containsExactly(holiday);
        verify(paginationValidator).validate(0, 10);
        assertThat(service.update(900001L, request)).isSameAs(holiday);
        service.delete(900001L);

        verify(repository).delete(holiday);
    }

    @Test
    void throwsTypedNotFoundForUpdateAndDelete() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(99L, request))
                .isInstanceOf(HolidayNotFoundException.class);
        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(HolidayNotFoundException.class);
    }
}
