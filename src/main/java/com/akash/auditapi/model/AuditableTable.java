package com.akash.auditapi.model;

import java.util.Arrays;
import java.util.List;

public enum AuditableTable {
    HOLIDAY_CALENDAR("PMC_HOLIDAY_CALENDAR", "Holiday Calendar"),
    LOCO_SINGAPORE("PMC_LOCO_SINGAPORE", "Loco Singapore"),
    POSITION_BALANCE("PMC_POSITION_BALANCE", "Position Balance");

    private final String tableName;
    private final String labelName;

    AuditableTable(String tableName, String labelName) {
        this.tableName = tableName;
        this.labelName = labelName;
    }

    public String tableName() {
        return tableName;
    }

    public String labelName() {
        return labelName;
    }

    public static List<String> labelNames() {
        return Arrays.stream(values()).map(AuditableTable::labelName).toList();
    }
}
