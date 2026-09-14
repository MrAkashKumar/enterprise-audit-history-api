package com.akash.auditapi.enums;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.akash.auditapi.exception.ApiMessages.duplicateAuditableTableName;

public enum AuditableTable {
    HOLIDAY_CALENDAR("HOLIDAY_CALENDAR", "Holiday Calendar"),
    LOCO_SINGAPORE("LOCO_SINGAPORE", "Loco Singapore"),
    POSITION_BALANCE("POSITION_BALANCE", "Position Balance");

    private static final List<String> LABEL_NAMES = Arrays.stream(values())
            .map(AuditableTable::labelName)
            .toList();
    private static final Map<String, String> TABLE_BY_PUBLIC_NAME = buildLookup();
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
        return LABEL_NAMES;
    }

    /** Resolves a public API label to its approved Oracle table name. */
    public static String resolveTableName(String labelOrTableName) {
        if (labelOrTableName == null) {
            return null;
        }
        String requested = labelOrTableName.trim();
        return TABLE_BY_PUBLIC_NAME.getOrDefault(requested.toUpperCase(Locale.ROOT), requested);
    }

    private static Map<String, String> buildLookup() {
        Map<String, String> lookup = new HashMap<>();
        for (AuditableTable table : values()) {
            register(lookup, table.tableName, table.tableName);
            register(lookup, table.labelName, table.tableName);
        }
        return Map.copyOf(lookup);
    }

    private static void register(Map<String, String> lookup, String publicName, String tableName) {
        String previous = lookup.put(publicName.toUpperCase(Locale.ROOT), tableName);
        if (previous != null && !previous.equals(tableName)) {
            throw new IllegalStateException(duplicateAuditableTableName(publicName));
        }
    }
}
