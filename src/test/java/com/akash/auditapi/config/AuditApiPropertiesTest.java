package com.akash.auditapi.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditApiPropertiesTest {
    @Test
    void appliesCentralDefaults() {
        AuditApiProperties properties = new AuditApiProperties(
                null, null, null, null, null, 0);

        assertThat(properties.sourceTablePrefix()).isEqualTo("PMC_");
        assertThat(properties.auditSuffix()).isEqualTo("_AUD");
        assertThat(properties.revisionTypeColumn()).isEqualTo("REVTYPE");
        assertThat(properties.maxPageSize()).isEqualTo(200);
    }

    @Test
    void rejectsUnsafeConfiguredIdentifierAndOracleInOverflow() {
        assertThatThrownBy(() -> new AuditApiProperties(
                "PMC_", "_AUD", "ID;DROP", "REV", "REVTYPE", 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuditApiProperties(
                "PMC_", "_AUD", "ID", "REV", "REVTYPE", 1001))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuditApiProperties(
                "PMC-%", "_AUD", "ID", "REV", "REVTYPE", 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsBlankValuesAsDefaults() {
        AuditApiProperties properties = new AuditApiProperties(
                " ", " ", " ", " ", " ", 10);

        assertThat(properties.sourceTablePrefix()).isEqualTo("PMC_");
        assertThat(properties.auditSuffix()).isEqualTo("_AUD");
        assertThat(properties.idColumn()).isEqualTo("ID");
        assertThat(properties.auditOrderColumn()).isEqualTo("REV");
        assertThat(properties.revisionTypeColumn()).isEqualTo("REVTYPE");
    }
}
