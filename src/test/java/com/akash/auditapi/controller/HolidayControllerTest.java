package com.akash.auditapi.controller;

import com.akash.auditapi.dto.request.HolidayRequest;
import com.akash.auditapi.dto.response.HolidayResponse;
import com.akash.auditapi.entity.Holiday;
import com.akash.auditapi.enums.ApiOutcomeCode;
import com.akash.auditapi.service.HolidayService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HolidayControllerTest {
    private final HolidayService service = mock(HolidayService.class);
    private final HolidayController controller = new HolidayController(service);

    @Test
    void createsHolidayWithCreatedStatusContract() {
        HolidayRequest request = new HolidayRequest(
                900001L, LocalDate.of(2030, 1, 1), "TST", "Test", "tester");
        Holiday holiday = new Holiday(
                request.id(), request.holidayDate(), request.calendarCode(), request.calendarName(), request.username());
        when(service.create(request)).thenReturn(holiday);

        var response = controller.create(request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(ApiOutcomeCode.SUCCESS);
        assertThat(response.getBody().getCode()).isEqualTo("2000");
        assertThat(response.getBody().getData().id()).isEqualTo(holiday.getId());
    }

    @Test
    void readsUpdatesAndDeletesUsingTheCommonContract() {
        HolidayRequest request = new HolidayRequest(
                900002L, LocalDate.of(2030, 2, 1), "TST2", "Test Two", "tester");
        Holiday holiday = new Holiday(
                request.id(), request.holidayDate(), request.calendarCode(), request.calendarName(), request.username());
        when(service.findAll(0, 10)).thenReturn(
                new PageImpl<>(List.of(holiday), PageRequest.of(0, 10), 1));
        when(service.update(holiday.getId(), request)).thenReturn(holiday);

        var pageResponse = controller.findAll(0, 10);
        var updateResponse = controller.update(holiday.getId(), request);
        var deleteResponse = controller.delete(holiday.getId());

        assertThat(pageResponse.getBody()).isNotNull();
        assertThat(pageResponse.getBody().getData().rows())
                .extracting(HolidayResponse::id)
                .containsExactly(holiday.getId());
        assertThat(updateResponse.getBody()).isNotNull();
        assertThat(updateResponse.getBody().getData().id()).isEqualTo(holiday.getId());
        assertThat(deleteResponse.getBody()).isNotNull();
        assertThat(deleteResponse.getBody().getData()).isNull();
        verify(service).delete(holiday.getId());
    }
}
