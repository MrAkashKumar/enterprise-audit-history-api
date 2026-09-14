package com.akash.auditapi.model;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditableTableTest {
    @Test
    void mapsOnlyOriginalTableNamesToLabels() {
        assertThat(AuditableTable.HOLIDAY_CALENDAR.tableName()).isEqualTo("HOLIDAY_CALENDAR");
        assertThat(AuditableTable.LOCO_SINGAPORE.tableName()).isEqualTo("LOCO_SINGAPORE");
        assertThat(AuditableTable.POSITION_BALANCE.tableName()).isEqualTo("POSITION_BALANCE");
        assertThat(AuditableTable.values())
                .extracting(AuditableTable::tableName)
                .noneMatch(tableName -> tableName.endsWith("_AUD"));
    }

    @Test
    void resolvesLabelsAndPhysicalNamesToApprovedTableNames() {
        assertThat(AuditableTable.resolveTableName("Loco Singapore")).isEqualTo("LOCO_SINGAPORE");
        assertThat(AuditableTable.resolveTableName("position balance")).isEqualTo("POSITION_BALANCE");
        assertThat(AuditableTable.resolveTableName("HOLIDAY_CALENDAR")).isEqualTo("HOLIDAY_CALENDAR");
        assertThat(AuditableTable.resolveTableName("UNKNOWN")).isEqualTo("UNKNOWN");
        assertThat(AuditableTable.resolveTableName(null)).isNull();
    }

    @Test
    void rejectsConflictingPublicNamesDuringLookupConstruction() {
        Map<String, String> lookup = new HashMap<>();
        ReflectionTestUtils.invokeMethod(AuditableTable.class, "register", lookup, "Label", "TABLE_ONE");
        ReflectionTestUtils.invokeMethod(AuditableTable.class, "register", lookup, "label", "TABLE_ONE");

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                AuditableTable.class, "register", lookup, "label", "TABLE_TWO"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate auditable table name or label");
    }
}
