package com.akash.auditapi.validation;

import com.akash.auditapi.config.AuditApiProperties;
import com.akash.auditapi.model.ApiOutcomeCode;
import com.akash.auditapi.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaginationValidatorTest {
    private final PaginationValidator validator = new PaginationValidator(new AuditApiProperties(
            "_AUD", "ID", "REV", "REVTYPE", Set.of(), 200));

    @Test
    void acceptsBoundaryValues() {
        assertThatCode(() -> validator.validate(0, 1)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(Integer.MAX_VALUE, 200)).doesNotThrowAnyException();
    }

    @Test
    void rejectsNegativePageAndInvalidSize() {
        assertThatThrownBy(() -> validator.validate(-1, 10))
                .isInstanceOfSatisfying(InvalidRequestException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ApiOutcomeCode.INVALID_PAGE_NO));
        for (int size : new int[]{0, 201}) {
            assertThatThrownBy(() -> validator.validate(0, size))
                    .isInstanceOfSatisfying(InvalidRequestException.class,
                            exception -> assertThat(exception.getCode()).isEqualTo(ApiOutcomeCode.INVALID_PAGE_SIZE));
        }
    }
}
