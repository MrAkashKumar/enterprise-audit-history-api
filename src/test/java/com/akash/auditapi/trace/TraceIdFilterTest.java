package com.akash.auditapi.trace;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static com.akash.auditapi.trace.TraceContext.HEADER_NAME;
import static com.akash.auditapi.trace.TraceContext.MDC_KEY;
import static org.assertj.core.api.Assertions.assertThat;

class TraceIdFilterTest {
    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    void propagatesValidTraceIdToMdcAndResponseThenClearsThreadContext() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/POSITION_BALANCE");
        request.addHeader(HEADER_NAME, "client-trace-123");
        var response = new MockHttpServletResponse();
        var traceSeenInsideChain = new AtomicReference<String>();

        filter.doFilter(request, response,
                (servletRequest, servletResponse) -> traceSeenInsideChain.set(MDC.get(MDC_KEY)));

        assertThat(traceSeenInsideChain).hasValue("client-trace-123");
        assertThat(response.getHeader(HEADER_NAME)).isNull();
        assertThat(MDC.get(MDC_KEY)).isNull();
    }

    @Test
    void replacesUnsafeTraceIdInsideMdcWithoutAddingSuccessHeader() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/POSITION_BALANCE");
        request.addHeader(HEADER_NAME, "invalid trace id\nforged-log-entry");
        var response = new MockHttpServletResponse();
        var traceSeenInsideChain = new AtomicReference<String>();

        filter.doFilter(request, response,
                (servletRequest, servletResponse) -> traceSeenInsideChain.set(MDC.get(MDC_KEY)));

        assertThat(traceSeenInsideChain.get()).matches("[0-9a-f]{32}");
        assertThat(response.getHeader(HEADER_NAME)).isNull();
        assertThat(MDC.get(MDC_KEY)).isNull();
    }
}
