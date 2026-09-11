package com.akash.auditapi.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditableTableTest {
    @Test
    void mapsOnlyOriginalTableNamesToLabels() {
        assertThat(AuditableTable.HOLIDAY_CALENDAR.tableName()).isEqualTo("PMC_HOLIDAY_CALENDAR");
        assertThat(AuditableTable.LOCO_SINGAPORE.tableName()).isEqualTo("PMC_LOCO_SINGAPORE");
        assertThat(AuditableTable.POSITION_BALANCE.tableName()).isEqualTo("PMC_POSITION_BALANCE");
        assertThat(AuditableTable.values())
                .extracting(AuditableTable::tableName)
                .noneMatch(tableName -> tableName.endsWith("_AUD"));
    }
}
