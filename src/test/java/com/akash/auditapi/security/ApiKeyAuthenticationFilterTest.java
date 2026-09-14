package com.akash.auditapi.security;

import com.akash.auditapi.exception.ApiErrorFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.akash.auditapi.trace.TraceIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiKeyAuthenticationFilterTest {
    private final ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(
            new ApiKeyProperties(true, "X-API-Key", "secret"), configuredObjectMapper(),
            new ApiErrorFactory());

    @Test
    void acceptsValidKeyAndRejectsMissingOrInvalidKey() throws Exception {
        MockHttpServletRequest accepted = request();
        accepted.addHeader("X-API-Key", "secret");
        MockHttpServletResponse acceptedResponse = new MockHttpServletResponse();
        filter.doFilter(accepted, acceptedResponse, new MockFilterChain());
        assertThat(acceptedResponse.getStatus()).isEqualTo(200);

        MockHttpServletResponse rejectedResponse = new MockHttpServletResponse();
        filter.doFilter(request(), rejectedResponse, new MockFilterChain());
        assertThat(rejectedResponse.getStatus()).isEqualTo(401);
        assertThat(rejectedResponse.getContentAsString()).contains("\"status\":\"UNAUTHORIZED\"");
        assertThat(rejectedResponse.getContentAsString()).contains("\"code\":\"4006\"");
        assertThat(rejectedResponse.getContentAsString()).contains("\"data\":null");
        assertThat(rejectedResponse.getContentAsString()).doesNotContain("\"path\"");
    }

    @Test
    void disabledSecuritySkipsAuthentication() throws Exception {
        ApiKeyAuthenticationFilter disabled = new ApiKeyAuthenticationFilter(
                new ApiKeyProperties(false, null, null), configuredObjectMapper(),
                new ApiErrorFactory());
        MockHttpServletResponse response = new MockHttpServletResponse();
        disabled.doFilter(request(), response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void skipsNonApiRoutesAndDefaultsBlankHeaderName() throws Exception {
        ApiKeyProperties properties = new ApiKeyProperties(true, " ", "secret");
        ApiKeyAuthenticationFilter defaultHeaderFilter = new ApiKeyAuthenticationFilter(
                properties, configuredObjectMapper(), new ApiErrorFactory());
        MockHttpServletResponse response = new MockHttpServletResponse();

        defaultHeaderFilter.doFilter(
                new MockHttpServletRequest("GET", "/actuator/health"),
                response, new MockFilterChain());

        assertThat(properties.headerName()).isEqualTo("X-API-Key");
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void enabledSecurityRequiresExternallyConfiguredSecret() {
        assertThatThrownBy(() -> new ApiKeyProperties(true, "X-API-Key", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AUDIT_API_KEY");
    }

    @Test
    void unauthorizedResponseUsesTraceIdCreatedByOuterTraceFilter() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new TraceIdFilter().doFilter(request(), response,
                (servletRequest, servletResponse) -> filter.doFilter(
                        servletRequest, servletResponse, new MockFilterChain()));

        String traceId = response.getHeader("X-Trace-Id");
        assertThat(traceId).isNotBlank();
        assertThat(response.getContentAsString()).contains("\"traceId\":\"" + traceId + "\"");
    }

    private MockHttpServletRequest request() {
        return new MockHttpServletRequest("GET", "/api/v1/POSITION_BALANCE");
    }

    private static ObjectMapper configuredObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
