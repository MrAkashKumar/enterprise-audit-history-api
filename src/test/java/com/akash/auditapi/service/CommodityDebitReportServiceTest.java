package com.akash.auditapi.service;

import com.akash.auditapi.dao.CommodityDebitRepository;
import com.akash.auditapi.entity.CommodityDebit;
import com.akash.auditapi.enums.ApiOutcomeCode;
import com.akash.auditapi.exception.CommodityDebitNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommodityDebitReportServiceTest {
    private final CommodityDebitRepository repository = mock(CommodityDebitRepository.class);
    private final CommodityDebitReportService service = new CommodityDebitReportService(
            repository, new PdfGenerationService(), new CommodityDebitPdfTemplate());

    @Test
    void loadsTheDatabaseRowAndGeneratesThePdf() {
        CommodityDebit entity = mock(CommodityDebit.class);
        when(entity.getId()).thenReturn(10L);
        when(entity.getTransactionReference()).thenReturn("TRN-10");
        when(repository.findById(10L)).thenReturn(Optional.of(entity));

        byte[] result = service.generate(10L);

        assertThat(result).startsWith("%PDF".getBytes());
        verify(repository).findById(10L);
    }

    @Test
    void throwsTheTypedNotFoundErrorWhenTheDatabaseRowDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generate(99L))
                .isInstanceOfSatisfying(CommodityDebitNotFoundException.class, exception -> {
                    assertThat(exception.getStatus().value()).isEqualTo(404);
                    assertThat(exception.getCode()).isEqualTo(ApiOutcomeCode.COMMODITY_DEBIT_NOT_FOUND);
                    assertThat(exception.getMessage()).isEqualTo("Commodity debit report data not found: 99");
                });
    }
}
