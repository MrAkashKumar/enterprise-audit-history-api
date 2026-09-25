package com.akash.auditapi.controller;

import com.akash.auditapi.service.CommodityDebitReportService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommodityDebitReportControllerTest {
    private final CommodityDebitReportService service = mock(CommodityDebitReportService.class);
    private final CommodityDebitReportController controller = new CommodityDebitReportController(service);

    @Test
    void returnsTheGeneratedPdfAsANonCacheableAttachment() {
        byte[] pdf = "%PDF-test".getBytes();
        when(service.generate(10L)).thenReturn(pdf);

        var response = controller.download(10L);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getContentLength()).isEqualTo(pdf.length);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"community.pdf\"");
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getBody()).isSameAs(pdf);
        verify(service).generate(10L);
    }
}
