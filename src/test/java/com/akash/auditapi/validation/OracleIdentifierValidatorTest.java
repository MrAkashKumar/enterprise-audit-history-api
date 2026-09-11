package com.akash.auditapi.validation;

import com.akash.auditapi.exception.ApiErrorCode;
import com.akash.auditapi.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OracleIdentifierValidatorTest {
    private final OracleIdentifierValidator validator = new OracleIdentifierValidator();

    @Test
    void trimsAndNormalizesValidIdentifier() {
        assertThat(validator.normalizeTableName(" pmc_position_balance "))
                .isEqualTo("PMC_POSITION_BALANCE");
    }

    @Test
    void rejectsNullQuotedSchemaAndInjectionIdentifiers() {
        for (String value : new String[]{null, "SCHEMA.TABLE", "\"MixedCase\"", "T;DELETE FROM X"}) {
            assertThatThrownBy(() -> validator.normalizeTableName(value))
                    .isInstanceOfSatisfying(InvalidRequestException.class,
                            exception -> assertThat(exception.getCode()).isEqualTo(ApiErrorCode.INVALID_TABLE_NAME));
        }
    }
}
