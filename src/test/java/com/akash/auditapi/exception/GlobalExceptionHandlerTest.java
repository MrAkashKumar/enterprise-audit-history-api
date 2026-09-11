package com.akash.auditapi.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static com.akash.auditapi.trace.TraceContext.MDC_KEY;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/TABLE");

    @Test
    void preservesTypedStatusCodeAndMessage() {
        MDC.put(MDC_KEY, "trace-error-123");
        var response = handler.auditApiError(new TableNotAllowedException("SECRET"));
        MDC.remove(MDC_KEY);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTraceId()).isEqualTo("trace-error-123");
        assertThat(response.getBody().getCode()).isEqualTo(ApiErrorCode.TABLE_NOT_ALLOWED);
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    void hidesDatabaseImplementationDetails() {
        var response = handler.databaseError(
                new DataRetrievalFailureException("ORA-00942 private detail"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("The database query could not be completed");
    }

    @Test
    void returnsEveryFieldValidationFailureWithoutNullableDereference() {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "name", "must not be blank"));
        bindingResult.addError(new FieldError("request", "date", "must be in the future"));
        var exception = new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

        var response = handler.bodyValidationError(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ApiErrorCode.VALIDATION_FAILED);
        assertThat(response.getBody().getDetails()).containsExactly(
                new ApiFieldError("name", "must not be blank"),
                new ApiFieldError("date", "must be in the future"));
    }

    @Test
    void convertsUnknownExceptionsToTraceableInternalErrors() {
        MDC.put(MDC_KEY, "trace-unknown-123");
        var response = handler.unexpectedError(new IllegalStateException("private detail"), request);
        MDC.remove(MDC_KEY);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ApiErrorCode.INTERNAL_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getTraceId()).isEqualTo("trace-unknown-123");
    }
}
