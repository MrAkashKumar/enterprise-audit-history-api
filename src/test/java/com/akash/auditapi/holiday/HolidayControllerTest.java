package com.akash.auditapi.holiday;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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
        assertThat(response.getBody().getCode().name()).isEqualTo("SUCCESS");
        assertThat(response.getBody().getData().id()).isEqualTo(holiday.getId());
    }
}
