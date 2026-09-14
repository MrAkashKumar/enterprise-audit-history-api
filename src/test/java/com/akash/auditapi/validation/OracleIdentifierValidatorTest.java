package com.akash.auditapi.validation;

import com.akash.auditapi.enums.ApiOutcomeCode;
import com.akash.auditapi.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OracleIdentifierValidatorTest {
    private final OracleIdentifierValidator validator = new OracleIdentifierValidator();

    @Test
    void trimsAndNormalizesValidIdentifier() {
        assertThat(validator.normalizeTableName(" position_balance "))
                .isEqualTo("POSITION_BALANCE");
    }

    @Test
    void rejectsNullQuotedSchemaAndInjectionIdentifiers() {
        for (String value : new String[]{null, "SCHEMA.TABLE", "\"MixedCase\"", "T;DELETE FROM X"}) {
            assertThatThrownBy(() -> validator.normalizeTableName(value))
                    .isInstanceOfSatisfying(InvalidRequestException.class,
                            exception -> assertThat(exception.getCode()).isEqualTo(ApiOutcomeCode.INVALID_TABLE_NAME));
        }
    }
}
