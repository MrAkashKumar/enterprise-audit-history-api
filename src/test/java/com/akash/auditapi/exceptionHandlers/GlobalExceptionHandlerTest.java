package com.akash.auditapi.exceptionHandlers;

import com.akash.auditapi.exception.ApiFieldError;
import com.akash.auditapi.exception.TableNotAllowedException;
import com.akash.auditapi.enums.ApiOutcomeCode;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void preservesTypedStatusCodeAndMessage() {
        var response = handler.auditApiError(new TableNotAllowedException("SECRET"));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(ApiOutcomeCode.TABLE_NOT_ALLOWED);
        assertThat(response.getBody().getCode()).isEqualTo("4007");
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    void hidesDatabaseImplementationDetails() {
        var response = handler.databaseError(
                new DataRetrievalFailureException("ORA-00942 private detail"));

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
        assertThat(response.getBody().getStatus()).isEqualTo(ApiOutcomeCode.VALIDATION_FAILED);
        assertThat(response.getBody().getCode()).isEqualTo("4004");
        assertThat(response.getBody().getDetails()).containsExactly(
                new ApiFieldError("name", "must not be blank"),
                new ApiFieldError("date", "must be in the future"));
    }

    @Test
    void substitutesMissingValidationMessages() {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "name", null));
        var exception = new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);

        var response = handler.bodyValidationError(exception);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetails())
                .containsExactly(new ApiFieldError("name", "Invalid value"));
    }

    @Test
    void convertsUnknownExceptionsToSafeInternalErrors() {
        var response = handler.unexpectedError(new IllegalStateException("private detail"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(ApiOutcomeCode.INTERNAL_ERROR);
        assertThat(response.getBody().getCode()).isEqualTo("5000");
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }
}
