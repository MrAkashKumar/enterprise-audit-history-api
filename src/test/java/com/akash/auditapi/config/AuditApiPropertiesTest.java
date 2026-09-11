package com.akash.auditapi.config;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditApiPropertiesTest {
    @Test
    void appliesCentralDefaultsAndNormalizesTables() {
        AuditApiProperties properties = new AuditApiProperties(
                null, null, null, null, Set.of("pmc_position_balance"), 0);

        assertThat(properties.auditSuffix()).isEqualTo("_AUD");
        assertThat(properties.revisionTypeColumn()).isEqualTo("REVTYPE");
        assertThat(properties.allowedTables()).containsExactly("PMC_POSITION_BALANCE");
        assertThat(properties.maxPageSize()).isEqualTo(200);
    }

    @Test
    void rejectsUnsafeConfiguredIdentifierAndOracleInOverflow() {
        assertThatThrownBy(() -> new AuditApiProperties(
                "_AUD", "ID;DROP", "REV", "REVTYPE", Set.of(), 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuditApiProperties(
                "_AUD", "ID", "REV", "REVTYPE", Set.of(), 1001))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
