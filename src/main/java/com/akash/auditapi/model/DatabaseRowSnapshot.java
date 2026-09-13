package com.akash.auditapi.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class DatabaseRowSnapshot {
    private DatabaseRowSnapshot() {
    }

    static Map<String, Object> copyOf(Map<String, Object> row) {
        // Map.copyOf rejects null database values and does not preserve column order.
        return Collections.unmodifiableMap(new LinkedHashMap<>(row));
    }
}
