package com.akash.auditapi.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApprovalPropertiesTest {
    @Test
    void appliesAndNormalizesColumnDefaults() {
        ApprovalProperties properties = new ApprovalProperties(null, " ", null);

        assertThat(properties.idColumn()).isEqualTo("ID");
        assertThat(properties.makerUsernameColumn()).isEqualTo("MAKER_USERNAME");
        assertThat(properties.checkerUsernameColumn()).isEqualTo("CHECKER_USERNAME");
    }

    @Test
    void normalizesConfiguredColumns() {
        ApprovalProperties properties = new ApprovalProperties("entity_id", "maker", "checker");

        assertThat(properties.idColumn()).isEqualTo("ENTITY_ID");
        assertThat(properties.makerUsernameColumn()).isEqualTo("MAKER");
        assertThat(properties.checkerUsernameColumn()).isEqualTo("CHECKER");
    }

    @Test
    void rejectsUnsafeColumns() {
        assertThatThrownBy(() -> new ApprovalProperties("ID;DROP", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
