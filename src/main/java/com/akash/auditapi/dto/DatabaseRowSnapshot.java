package com.akash.auditapi.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Creates immutable snapshots of dynamic database rows while preserving nulls and column order.
 * Response DTOs use it to avoid exposing mutable JDBC maps.
 */
public final class DatabaseRowSnapshot {
    private DatabaseRowSnapshot() {
    }

    public static Map<String, Object> copyOf(Map<String, Object> row) {
        // Map.copyOf rejects null database values and does not preserve column order.
        return Collections.unmodifiableMap(new LinkedHashMap<>(row));
    }
}
